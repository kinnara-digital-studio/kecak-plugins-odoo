package com.kinnarastudio.kecakplugins.odoo.common.property;

import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.workflow.model.WorkflowAssignment;

public final class OdooAuthorizationUtil {
    private OdooAuthorizationUtil() {}
    public static String getBaseUrl(ExtDefaultPlugin plugin) {
        WorkflowAssignment assignment = (WorkflowAssignment) plugin.getProperty("workflowAssignment");
        return processHashVariable(plugin.getPropertyString("baseUrl"), assignment);
    }

    public static String getDatabase(ExtDefaultPlugin plugin) {
        WorkflowAssignment assignment = (WorkflowAssignment) plugin.getProperty("workflowAssignment");
        return processHashVariable(plugin.getPropertyString("database"), assignment);
    }

    /**
     * Get username from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getUsername(ExtDefaultPlugin plugin) {
        WorkflowAssignment assignment = (WorkflowAssignment) plugin.getProperty("workflowAssignment");
        return processHashVariable(plugin.getPropertyString("user"), assignment);
    }

    /**
     * Get apiKey from OdooAuthorization.json
     *
     * @param plugin
     * @return
     */
    public static String getApiKey(ExtDefaultPlugin plugin) {
        WorkflowAssignment assignment = (WorkflowAssignment) plugin.getProperty("workflowAssignment");
        return processHashVariable(plugin.getPropertyString("apiKey"), assignment);
    }

    public static String getModel(ExtDefaultPlugin plugin) {
        WorkflowAssignment assignment = (WorkflowAssignment) plugin.getProperty("workflowAssignment");
        return processHashVariable(plugin.getPropertyString("model"), assignment);
    }

    private static String processHashVariable(String value, WorkflowAssignment assignment) {
        String processedValue;
        int limit = 10;
        do {
            processedValue = AppUtil.processHashVariable(value, assignment, null, null);
            limit--;
        } while(!processedValue.equals(value) && limit > 0);

        return processedValue;
    }
}
