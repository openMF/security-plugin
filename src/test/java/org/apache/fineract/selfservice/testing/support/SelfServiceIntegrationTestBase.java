/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.testing.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import java.time.Duration;

public abstract class SelfServiceIntegrationTestBase {

    private static final Network network = Network.newNetwork();

    // 1. Boot Postgres with the default tenant DB
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("db")
            .withDatabaseName("fineract_default")
            // Use 'postgres' superuser to easily create the second DB
            .withUsername("postgres") 
            .withPassword("postgres");

    protected static final GenericContainer<?> fineract;

    static {
        postgres.start();

        // 2. Pre-Initialize the Master Tenant Database
        try {
            postgres.execInContainer("psql", "-U", "postgres", "-c", "CREATE DATABASE fineract_tenants;");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize fineract_tenants database in Testcontainers", e);
        }

        // 3. Fineract Container with Strict Env Variables
        fineract = new GenericContainer<>(DockerImageName.parse("apache/fineract:latest")) // Adjust version if needed
                .withNetwork(network)
                .withExposedPorts(8080)
                
                // Hikari Master Configuration (fineract_tenants)
                .withEnv("FINERACT_HIKARI_JDBC_URL", "jdbc:postgresql://db:5432/fineract_tenants")
                .withEnv("FINERACT_HIKARI_USERNAME", "postgres")
                .withEnv("FINERACT_HIKARI_PASSWORD", "postgres")
                .withEnv("FINERACT_HIKARI_DRIVER_SOURCE_CLASS_NAME", "org.postgresql.Driver")
                
                // Default Tenant Database Properties (fineract_default)
                .withEnv("FINERACT_DEFAULT_TENANTDB_HOSTNAME", "db")
                .withEnv("FINERACT_DEFAULT_TENANTDB_PORT", "5432")
                .withEnv("FINERACT_DEFAULT_TENANTDB_UID", "postgres")
                .withEnv("FINERACT_DEFAULT_TENANTDB_PWD", "postgres")
                .withEnv("FINERACT_DEFAULT_TENANTDB_CONN_PARAMS", "")
                
                // Timezone (Optional but highly recommended to prevent sync errors)
                .withEnv("TZ", "UTC")
                .withEnv("JAVA_TOOL_OPTIONS", "-Xmx2G")
                
                // Disable SSL so Testcontainers can hit port 8080 via HTTP
                .withEnv("FINERACT_SERVER_SSL_ENABLED", "false")
                .withEnv("FINERACT_SERVER_PORT", "8080")
                
                // Mount the compiled Plugin JAR
                .withCopyFileToContainer(
                        MountableFile.forHostPath("target/selfservice-plugin-1.15.0-SNAPSHOT.jar"), 
                        "/app/libs/selfservice-plugin-1.15.0-SNAPSHOT.jar"
                )
                
                // Wait for the Tomcat server to finish Liquibase and report healthy
                .waitingFor(Wait.forHttp("/fineract-provider/actuator/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5)));
        
        fineract.start();
    }

    protected static int getFineractPort() {
        return fineract.getMappedPort(8080);
    }
}
