package com.kinnarastudio.kecakplugins.odoo.common.property;

import org.joget.plugin.base.ExtDefaultPlugin;

public class OdooDataListActionUtil {
    private OdooDataListActionUtil() {
    }

    public static String getLabel(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("label");
    }

    public static String getConfirmation(ExtDefaultPlugin plugin) {
        return ifEmpty(plugin.getPropertyString("confirmation"), "Are you sure?");
    }

    public static String getHref(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("href");
    }

    private static String ifEmpty(String value, String ifEmpty) {
        return value == null || value.isEmpty() ? ifEmpty : value;
    }
}
