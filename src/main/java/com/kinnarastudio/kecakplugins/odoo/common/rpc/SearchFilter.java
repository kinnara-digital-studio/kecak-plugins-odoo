package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public class SearchFilter {
    public final static String EQUAL = "=";
    public final static String NOT_EQUAL = "<>";
    public final static String GREATER = ">";
    public final static String GREATER_EQUAL = ">=";
    public final static String LESS = "<";
    public final static String LESS_EQUAL = "<=";
    public final static String IN = "in";
    public final static String AND = "AND";
    public final static String OR = "OR";
    private final String field;
    private final String operator;
    private final Object value;
    private final String join;

    public SearchFilter(String field, String operator, Object value, String join) {
        this.field = field;
        this.operator = operator;
        this.value = (value == null || "null".equals(value)) ? false : value;
        this.join = join;
    }

    public SearchFilter(String field, String operator, Object value) {
        this(field, operator, value, null);
    }

    public SearchFilter(String field, Object value) {
        this(field, EQUAL, value);
    }

    public SearchFilter(String field, String join, Object... values) {
        this.field = field;
        this.operator = IN;
        this.value = values;
        this.join = join;
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

    public String getJoin() {
        return join;
    }

    public static SearchFilter[] single(String field, Object value) {
        return new SearchFilter[] { new SearchFilter(field, value) };
    }
}
