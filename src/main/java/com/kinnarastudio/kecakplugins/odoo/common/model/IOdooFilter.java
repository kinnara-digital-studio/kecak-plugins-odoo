package com.kinnarastudio.kecakplugins.odoo.common.model;

import com.kinnarastudio.odooxmlrpc.model.DataType;

public interface IOdooFilter {
    String getField();

    String getOperator();

    String getValue();

    DataType getDataType();
}
