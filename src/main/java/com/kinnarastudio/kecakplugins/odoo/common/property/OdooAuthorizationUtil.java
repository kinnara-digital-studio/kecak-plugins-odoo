package com.kinnarastudio.kecakplugins.odoo.common.property;

import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.ExtDefaultPlugin;

public final class OdooAuthorizationUtil {
    private OdooAuthorizationUtil() {}
    public static String getBaseUrl(ExtDefaultPlugin plugin) {
        return AppUtil.processHashVariable(plugin.getPropertyString("baseUrl"), null, null, null);
    }

    public static String getDatabase(ExtDefaultPlugin plugin) {
        return AppUtil.processHashVariable(plugin.getPropertyString("database"), null, null, null);
    }

    /**
     * Get username from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getUsername(ExtDefaultPlugin plugin) {
        return AppUtil.processHashVariable(plugin.getPropertyString("user"), null, null, null);
    }

    /**
     * Get apiKey from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getApiKey(ExtDefaultPlugin plugin) {
        return AppUtil.processHashVariable(plugin.getPropertyString("apiKey"), null, null, null);
    }

    public static String getModel(ExtDefaultPlugin plugin) {
        return AppUtil.processHashVariable(plugin.getPropertyString("model"), null, null, null);
    }
}
