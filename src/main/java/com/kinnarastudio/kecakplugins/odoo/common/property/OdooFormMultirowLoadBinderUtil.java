package com.kinnarastudio.kecakplugins.odoo.common.property;

import org.joget.plugin.base.ExtDefaultPlugin;

public class OdooFormMultirowLoadBinderUtil {
    private OdooFormMultirowLoadBinderUtil(){}

    public static String getForeignKeyField(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("foreignKeyField");
    }
}
