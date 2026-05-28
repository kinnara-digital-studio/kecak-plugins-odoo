package com.kinnarastudio.kecakplugins.odoo.common.rpc;

/**
 * Odoo field data types
 */
public enum DataType {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    MANY2ONE,
    MANY2MANY;

    /**
     * Get {@link DataType} from string value
     * @param value string value
     * @return {@link DataType}, default {@link #STRING}
     */
    public static DataType parse(String value) {
        for (DataType dataType : DataType.values()) {
            if (dataType.name().equalsIgnoreCase(value)) {
                return dataType;
            }
        }

        return STRING;
    }
}
