package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;

public interface IOdooFilter {
    String getField();

    SearchFilter.Operator getFilterOperator();

    String getValue();

    DataType getDataType();
}
