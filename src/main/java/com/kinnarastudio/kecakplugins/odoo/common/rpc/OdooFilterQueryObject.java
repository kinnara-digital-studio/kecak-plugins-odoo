package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import org.joget.apps.datalist.model.DataListFilterQueryObject;

@Deprecated
public class OdooFilterQueryObject extends DataListFilterQueryObject implements IOdooFilter {
    private final String field;
    private final String value;

    private final SearchFilter.Operator operator;
    private final DataType dataType;

    public OdooFilterQueryObject(String field, SearchFilter.Operator operator, String value, DataType dataType) {
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
        return operator.name();
    }

    @Override
    public SearchFilter.Operator getFilterOperator() {
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
