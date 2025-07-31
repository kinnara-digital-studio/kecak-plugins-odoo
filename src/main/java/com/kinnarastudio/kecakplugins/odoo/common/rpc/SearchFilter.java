package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public class SearchFilter {
    public final static String EQUAL = "=";
    public final static String NOT_EQUAL = "<>";
    public final static String GREATER = ">";
    public final static String GREATER_EQUAL = ">=";
    public final static String LESS = "<";
    public final static String LESS_EQUAL = "<=";
    private final String field;
    private final String operator;
    private final Object value;

    public SearchFilter(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public SearchFilter(String field, Object value) {
        this(field, EQUAL, value);
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }
}
