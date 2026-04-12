/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.testing.support;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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
        //
        // Image selection (first match wins): -Dfineract.it.image, then env FINERACT_IT_IMAGE, else
        // apache/fineract:develop (Docker Hub — tracks Fineract develop, aligned with README and
        // mifosx/docker-compose.yml). Use FINERACT_IT_IMAGE (-Dfineract.it.image) to pin a digest or a
        // locally built image that matches pom fineract.version exactly.
        final String fromEnv = System.getenv("FINERACT_IT_IMAGE");
        final String fromProperty = System.getProperty("fineract.it.image");
        final String defaultImage = "apache/fineract:develop";
        final String fineractImage =
            fromProperty != null && !fromProperty.isBlank()
                ? fromProperty
                : (fromEnv != null && !fromEnv.isBlank() ? fromEnv : defaultImage);
        final DockerImageName dockerImageName =
            DockerImageName.parse(fineractImage).asCompatibleSubstituteFor("apache/fineract");

        fineract = new GenericContainer<>(dockerImageName)
                .withNetwork(network)
                .withExposedPorts(8443)
                
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
                
                // Enable the self-service module (matches Fineract docker-compose.override.yml)
                .withEnv("FINERACT_MODULE_SELFSERVICE_ENABLED", "true")
                .withEnv("SPRING_MAIN_ALLOW_BEAN_DEFINITION_OVERRIDING", "true")
                .withEnv("FINERACT_MODULES_SELFSERVICE_RUNREPORTS_ALLOWLIST", "Client Details")
                
                // Timezone (Optional but highly recommended to prevent sync errors)
                .withEnv("TZ", "UTC")
                .withEnv("JAVA_TOOL_OPTIONS", "-Xmx2G")
                
                // Keep SSL enabled (default for Fineract container images); tests use relaxed HTTPS validation.
                .withEnv("FINERACT_SERVER_SSL_ENABLED", "true")
                .withEnv("FINERACT_SERVER_PORT", "8443")
                
                // Mount the compiled Plugin JAR into the auto-scanned /app/plugins directory
                .withCopyFileToContainer(
                        MountableFile.forHostPath("target/selfservice-plugin-1.15.0-SNAPSHOT.jar"), 
                        "/app/plugins/selfservice-plugin.jar"
                )
                
                // Prepend the plugin JAR to the JIB container's classpath.
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint(
                            "sh", "-c",
                            "CLASSPATH=$(cat /app/jib-classpath-file) && " +
                            "exec java $JAVA_TOOL_OPTIONS " +
                            "-Duser.home=/tmp -Dfile.encoding=UTF-8 -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom " +
                            "-cp /app/plugins/selfservice-plugin.jar:$CLASSPATH " +
                            "org.apache.fineract.ServerApplication"
                    );
                    cmd.withCmd();
                })
                        
                // HostPortWaitStrategy still runs an internal port check that requires bash (missing in
                // apache/fineract images). HTTPS + allowInsecure waits until the app serves actuator.
                .waitingFor(
                        Wait.forHttps("/fineract-provider/actuator/health")
                                .allowInsecure()
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(7)));
        
        fineract.start();
    }

    protected static int getFineractPort() {
        return fineract.getMappedPort(8443);
    }
}
