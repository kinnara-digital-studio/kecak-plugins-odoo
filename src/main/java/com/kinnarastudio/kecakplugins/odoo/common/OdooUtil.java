package com.kinnarastudio.kecakplugins.odoo.common;

import org.joget.plugin.base.ExtDefaultPlugin;

public class OdooUtil {
    public static String getBaseUrl(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("baseUrl");
    }

    public static String getDatabase(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("database");
    }

    /**
     * Get username from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getUsername(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("user");
    }

    /**
     * Get apiKey from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getApiKey(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("apiKey");
    }

    public static String getModel(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("model");
    }
}
