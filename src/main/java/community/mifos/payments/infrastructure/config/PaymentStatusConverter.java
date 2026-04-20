/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.config;

import community.mifos.payments.core.domain.PaymentStatus;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 * EclipseLink converter for PaymentStatus enum.
 * Maps enum to VARCHAR database column.
 */
public class PaymentStatusConverter implements Converter {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public Object convertObjectValueToDataValue(Object objectValue, Session session) {
        if (objectValue == null) {
            return null;
        }
        return ((PaymentStatus) objectValue).getDatabaseValue();
    }
    
    @Override
    public Object convertDataValueToObjectValue(Object dataValue, Session session) {
        if (dataValue == null) {
            return null;
        }
        return PaymentStatus.fromDatabaseValue((String) dataValue);
    }
    
    @Override
    public boolean isMutable() {
        return false;
    }
    
    @Override
    public void initialize(DatabaseMapping mapping, Session session) {
        // No initialization required
    }
}

