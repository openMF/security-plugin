/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.selfservice.client.service;

import org.apache.fineract.selfservice.registration.data.IdentityDocumentData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SelfServiceClientIdentifierReadPlatformServiceImpl implements SelfServiceClientIdentifierReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    
    @Override
    public List<IdentityDocumentData> retrieveClientIdentifiers() {

        final ClientIdentityMapper rm = new ClientIdentityMapper();

        String sql = "select " + rm.publicSchema();
        
        log.info("SELECT IDENTITY DOCUMENTS "+sql);
        
        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }

    private static final class ClientIdentityMapper implements RowMapper<IdentityDocumentData> {

        ClientIdentityMapper() {}

        public String publicSchema() {
            return  "cv.id AS code_value_id, " +
                    "cv.code_value AS identifier_type, " +
                    "cv.code_description, " +
                    "cv.order_position, " +
                    "cv.is_active " +
                    "FROM m_code_value cv " +
                    "JOIN m_code c ON cv.code_id = c.id " +
                    "WHERE c.id = 1 " + // Customer Identifier
                    "AND cv.is_active = true " +
                    "ORDER BY cv.order_position ";
        }

        @Override
        public IdentityDocumentData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long documentId = JdbcSupport.getLong(rs, "code_value_id");
            final String documentType = rs.getString("identifier_type");
            final String documentDescription = rs.getString("code_description");
            final Integer documentPosition = rs.getInt("order_position");
            final Boolean documentStatus = rs.getBoolean("is_active");            
            return IdentityDocumentData.singleItem(documentId, documentType, documentDescription, documentPosition, documentStatus);
        }

    }

}
