package com.kinnarastudio.kecakplugins.odoo.tool;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooRpcToolUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Execute Odoo RPC
 */
public class OdooRpcTool extends DefaultApplicationPlugin {
    public final static String LABEL = "Odoo RPC Tool";

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public Object execute(Map properties) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final String method = OdooRpcToolUtil.getMethod(this);
        final Optional<Integer> optRecordId = OdooRpcToolUtil.optRecordId(this);
        final Map<String, Object> record = OdooRpcToolUtil.getRecord(this);

        try {
            switch (method) {
                case "create": {
                    final int resultValue = rpc.create(model, record);

                    final String resultWorkflowVariable = OdooRpcToolUtil.getResultWorkflowVariable(this);

                    final WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                    final WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
                    workflowManager.activityVariable(workflowAssignment.getActivityId(), resultWorkflowVariable, String.valueOf(resultValue));

                    break;
                }

                case "write":
                    rpc.write(model, optRecordId.orElseThrow(() -> new OdooCallMethodException("Record ID is required for [" + method + "] method")), record);
                    break;

                case "unlink":
                    rpc.unlink(model, optRecordId.orElseThrow(() -> new OdooCallMethodException("Record ID is required for [" + method + "] method")));
                    break;

                default:
                    throw new OdooCallMethodException("Method [" + method + "] is not understood");
            }
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
        }
        return null;
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] resources = new String[]{
                "/properties/common/OdooAuthorization.json",
                "/properties/tool/OdooRpcTool.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }
}
