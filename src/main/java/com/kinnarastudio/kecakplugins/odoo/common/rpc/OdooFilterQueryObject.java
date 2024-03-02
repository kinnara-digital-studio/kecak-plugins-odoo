package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import org.joget.apps.datalist.model.DataListFilterQueryObject;

public class OdooFilterQueryObject extends DataListFilterQueryObject implements IOdooFilter {
    private final String field;

    public OdooFilterQueryObject(String field) {
        this.field = field;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public String getOperator() {
        return "=";
    }

    @Override
    public String getValue() {
        return null;
    }
}
