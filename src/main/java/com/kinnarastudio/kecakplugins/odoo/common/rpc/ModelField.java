package com.kinnarastudio.kecakplugins.odoo.common.rpc;

public class ModelField {
    private final String string;

    private final String type;

    private final String help;

    public ModelField(String string, String type, String help) {
        this.string = string;
        this.type = type;
        this.help = help;
    }

    public String getString() {
        return string;
    }

    public String getType() {
        return type;
    }

    public String getHelp() {
        return help;
    }
}
