package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import org.joget.apps.datalist.model.DataListFilterQueryObject;

public class OdooFilterQueryObject extends DataListFilterQueryObject implements IOdooFilter {
    private final String field;
    private final String value;

    private final String operator;
    private final DataType dataType;

    public OdooFilterQueryObject(String field, String operator, String value, DataType dataType) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.dataType = dataType;
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

    @Override
    public DataType getDataType() {
        return dataType;
    }
}
