package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public class SearchFilter {
    public final static String EQUAL = "=";
    public final static String NOT_EQUAL = "<>";
    public final static String GREATER = ">";
    public final static String GREATER_EQUAL = ">=";
    public final static String LESS = "<";
    public final static String LESS_EQUAL = "<=";
    public final static String IN = "in";
    private final String field;
    private final String operator;
    private final Object[] values;
    private final boolean negate;

    public SearchFilter(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.values = new Object[] { value };
        this.negate = false;
    }

    public SearchFilter(String field, Object value) {
        this(field, EQUAL, value);
    }

    public SearchFilter(String field, Object... values) {
        this.field = field;
        this.operator = IN;
        this.values = values;
        this.negate = false;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return (negate ? "not " : "") + operator;
    }

    public Object[] getValues() {
        return values;
    }
}
