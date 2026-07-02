package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import com.kinnarastudio.commons.Try;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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
     *
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

    public <T> T valueParser(Object rawValue) throws NumberFormatException {
        switch (this) {
            case STRING:
                return (T) String.valueOf(rawValue);
            case MANY2ONE:
            case INTEGER:
                return (T) Integer.valueOf(rawValue.toString());
            case FLOAT:
                return (T) Float.valueOf(rawValue.toString());
            case BOOLEAN:
                return (T) Boolean.valueOf(rawValue.toString().toLowerCase());
            case MANY2MANY:
                return (T) Optional.of(rawValue)
                        .map(String::valueOf)
                        .filter(Predicate.not(String::isEmpty))
                        .map(s -> s.split(";"))
                        .stream()
                        .flatMap(Arrays::stream)
                        .map(String::trim)
                        .filter(Predicate.not(String::isEmpty))
                        .map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> null))
                        .filter(Objects::nonNull)
                        .toArray(Integer[]::new);

            default:
                return (T) rawValue.toString();
        }
    }
}
