package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import org.joget.apps.datalist.model.DataListFilterQueryObject;

public class OdooFilterQueryObject extends DataListFilterQueryObject implements IOdooFilter {
    private final String field;
    private final String value;

    private final String operator;

    public OdooFilterQueryObject(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public String getValue() {
        return value;
    }
}
