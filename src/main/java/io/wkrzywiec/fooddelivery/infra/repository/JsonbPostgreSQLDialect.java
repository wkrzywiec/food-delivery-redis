package io.wkrzywiec.fooddelivery.infra.repository;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

public class JsonbPostgreSQLDialect extends PostgreSQL10Dialect {

    public JsonbPostgreSQLDialect() {
        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
    }
}
