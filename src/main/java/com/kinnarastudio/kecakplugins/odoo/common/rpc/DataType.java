package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public enum DataType {
    STRING,
    INTEGER;

    public static DataType parse(String string) {
        switch (string) {
            case "integer":
            case "many2one":
                return DataType.INTEGER;
            default:
                return DataType.STRING;
        }
    }
}
