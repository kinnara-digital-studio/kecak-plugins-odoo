package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public class SearchFilter {
    public final static String EQ = "=";
    private final String field;
    private final String operator;
    private final Object value;

    public SearchFilter(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public SearchFilter(String field, Object value) {
        this(field, EQ, value);
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
