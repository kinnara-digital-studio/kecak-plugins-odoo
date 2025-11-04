package com.kinnarastudio.kecakplugins.odoo.process;

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

import java.util.*;
import java.util.function.Predicate;

/**
 * Execute Odoo RPC
 *
 * @author aristo
 * Implementing {@link OdooRpc#messagePost(String, int, String)}
 * @since 2025-11-03
 *
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

        final Map<String, Object> parsedRecord = new HashMap<>();

        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();

            String dataType = String.valueOf(valueMap.get("dataType"));
            Object rawValue = valueMap.get("value");

            if (dataType != null && rawValue != null) {
                switch (dataType.toLowerCase()) {
                    case "string":
                        parsedRecord.put(key, String.valueOf(rawValue));
                        break;
                    case "integer":
                        parsedRecord.put(key, Integer.valueOf(rawValue.toString()));
                        break;
                    case "float":
                        parsedRecord.put(key, Float.valueOf(rawValue.toString()));
                        break;
                    case "boolean":
                        parsedRecord.put(key, Boolean.valueOf(rawValue.toString().toLowerCase()));
                        break;
                    default:
                        parsedRecord.put(key, rawValue.toString());
                }
            }
        }

        long delayInSeconds = getDelayedExecutionTime();
        runDelayed(Try.onRunnable(() -> {
            final int recordId;

            switch (method) {
                case "create": {
                    recordId = rpc.create(model, parsedRecord);

                    final String resultWorkflowVariable = OdooRpcToolUtil.getResultWorkflowVariable(this);

                    final WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                    final WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
                    workflowManager.activityVariable(workflowAssignment.getActivityId(), resultWorkflowVariable, String.valueOf(recordId));

                    break;
                }

                case "write":
                    recordId = optRecordId.orElseThrow(() -> new OdooCallMethodException("Record ID is required for [" + method + "] method"));

                    try {
                        rpc.write(model, recordId, parsedRecord);
                    } catch (OdooCallMethodException e) {
                        final String postErrorMessage = getPostErrorMessage();
                        if (!postErrorMessage.isEmpty()) {
                            rpc.messagePost(model, recordId, postErrorMessage);
                        }

                        throw e;
                    }

                    break;

                case "unlink":
                    recordId = optRecordId.orElseThrow(() -> new OdooCallMethodException("Record ID is required for [" + method + "] method"));

                    try {
                        rpc.unlink(model, recordId);
                    } catch (OdooCallMethodException e) {
                        final String postErrorMessage = getPostErrorMessage();
                        if (!postErrorMessage.isEmpty()) {
                            rpc.messagePost(model, recordId, postErrorMessage);
                        }

                        throw e;
                    }

                    break;

                case "messagePost":
                    recordId = optRecordId.orElseThrow(() -> new OdooCallMethodException("Record ID is required for [" + method + "] method"));
                    rpc.messagePost(model, recordId, getMessagePost());

                default:
                    throw new OdooCallMethodException("Method [" + method + "] is not understood");
            }

        }), delayInSeconds);
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
        final String[] resources = new String[]{"/properties/common/OdooAuthorization.json", "/properties/process/OdooRpcTool.json"};

        return Arrays.stream(resources).map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere")).map(Try.onFunction(JSONArray::new)).flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject))).collect(JSONCollectors.toJSONArray()).toString();
    }

    /**
     * Get delayed execution time in seconds
     *
     * @return
     */
    protected long getDelayedExecutionTime() {
        return Optional.of("delay").map(this::getPropertyString).filter(Predicate.not(String::isEmpty)).map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> 0)).orElse(0);
    }

    /**
     * Run runnable after delay time
     *
     * @param runnable
     * @param delayInSeconds
     */
    protected void runDelayed(Runnable runnable, long delayInSeconds) {
        if (delayInSeconds > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(delayInSeconds * 1000);
                    runnable.run();
                } catch (InterruptedException e) {
                    LogUtil.error(getClassName(), e, e.getMessage());
                }
            }).start();
        } else {
            runnable.run();
        }
    }

    protected String getMessagePost() {
        return getPropertyString("messagePost");
    }

    protected String getPostSuccessMessage() {
        return getPropertyString("postSuccessMessage");
    }

    protected String getPostErrorMessage() {
        return getPropertyString("postErrorMessage");
    }
}
