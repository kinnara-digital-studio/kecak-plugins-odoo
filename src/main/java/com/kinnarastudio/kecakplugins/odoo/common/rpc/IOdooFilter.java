package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public interface IOdooFilter {
    String getField();

    String getOperator();

    String getValue();

    DataType getDataType();

    enum DataType {
        STRING,
        INTEGER
    }
}
