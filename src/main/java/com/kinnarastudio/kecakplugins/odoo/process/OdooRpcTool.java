package com.kinnarastudio.kecakplugins.odoo.process;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.app.webservice.OdooTestConnectionWebService;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooRpcToolUtil;
import com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException;
import com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException;
import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.rpc.OdooRpc;
import org.apache.commons.lang3.tuple.Pair;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        final WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");

        try {
            final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
            final String database = OdooAuthorizationUtil.getDatabase(this);
            final String user = OdooAuthorizationUtil.getUsername(this);
            final String apiKey = OdooAuthorizationUtil.getApiKey(this);
            final String model = OdooAuthorizationUtil.getModel(this);

            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

            final String method = OdooRpcToolUtil.getMethod(this);
            final Optional<Integer> optRecordId = OdooRpcToolUtil.optRecordId(this);
            final Map<String, Pair<DataType, String>> record = OdooRpcToolUtil.getRecord(this);

            final Map<String, Object> parsedRecord = new HashMap<>();

            for (Map.Entry<String, Pair<DataType, String>> entry : record.entrySet()) {
                String key = entry.getKey();
                Pair<DataType, String> typedValue = entry.getValue();

                DataType dataType = typedValue.getLeft();
                Object rawValue = AppUtil.processHashVariable(typedValue.getRight(), workflowAssignment, null, null);

                if (dataType != null && rawValue != null) {
                    parsedRecord.put(key, dataType.valueParser(rawValue));
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
        } catch (OdooAuthorizationException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
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
        final JSONArray jsonDataType = Arrays.stream(DataType.values())
                .map(Enum::name)
                .map(s -> new JSONObject() {{
                    try {
                        put(FormUtil.PROPERTY_VALUE, s);
                        put(FormUtil.PROPERTY_LABEL, s);
                    } catch (JSONException ignored) {
                    }
                }})
                .collect(JSONCollectors.toJSONArray());

        final Object[] argsOdooAuth = new Object[]{OdooTestConnectionWebService.class.getName()};
        final Object[] argsTool = new Object[]{jsonDataType.toString()};

        final Pair<String, Object[]>[] resources = new Pair[]{
                Pair.of("/properties/common/OdooAuthorization.json", argsOdooAuth),
                Pair.of("/properties/process/OdooRpcTool.json", argsTool)
        };

        return Arrays.stream(resources)
                .map(pair -> AppUtil.readPluginResource(getClassName(), pair.getLeft(), pair.getRight(), true, ""))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    /**
     * Get delayed execution time in seconds
     *
     * @return
     */
    protected long getDelayedExecutionTime() {
        return Optional.of("delay")
                .map(this::getPropertyString)
                .filter(Predicate.not(String::isEmpty))
                .map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> 0))
                .orElse(0);
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
