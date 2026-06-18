package com.kinnarastudio.kecakplugins.odoo.process;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.app.webservice.OdooTestConnectionWebService;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooUtilityException;
import org.apache.commons.lang3.tuple.Pair;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.app.service.AuditTrailManager;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilter;
import org.joget.apps.datalist.service.DataListService;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.json.JSONArray;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Execute Odoo RPC with DataList rows as parameters
 */
public class OdooDataListRpcTool extends DefaultApplicationPlugin {
    public final static String LABEL = "Odoo DataList RPC Tool";

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
    public Object execute(Map map) {
        AuditTrailManager auditTrailManager = (AuditTrailManager) AppUtil.getApplicationContext().getBean("auditTrailManager");

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey, auditTrailManager);

        final WorkflowAssignment workflowAssignment = (WorkflowAssignment) map.get("workflowAssignment");
        final DataList dataList;
        try {
            dataList = getDataList(workflowAssignment);
        } catch (OdooUtilityException e) {
            throw new RuntimeException(e);
        }
        final DataListCollection<Map<String, Object>> dataListRows = dataList.getRows();
        if (dataListRows == null) {
            LogUtil.info(getClassName(), "Rows not found");
            return null;
        }

        dataListRows.forEach(Try.onConsumer(m -> {
            Map<String, Object> odooRow = remapToOdooRow(m);
            int recordId = rpc.create(model, odooRow);
            LogUtil.info(getClassName(), "Record [" + recordId + "] has been created in model [" + model + "]");
        }));

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
        final Object[] argsOdooAuth = new Object[]{OdooTestConnectionWebService.class.getName()};
        final Object[] argsOdooTool = null;

        final Pair<String, Object[]>[] resources = new Pair[]{
                Pair.of("/properties/common/OdooAuthorization.json", argsOdooAuth),
                Pair.of("/properties/process/OdooDataListRpcTool.json", argsOdooTool)
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

    protected DataList getDataList(WorkflowAssignment workflowAssignment) throws OdooUtilityException {
        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        final String datalistId = getDataListId();

        final DataListService dataListService = (DataListService) appContext.getBean("dataListService");
        final DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) appContext.getBean("datalistDefinitionDao");
        final DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(datalistId, appDef);

        final DataList dataList = Optional.ofNullable(datalistDefinition)
                .map(DatalistDefinition::getJson)
                .map(s -> AppUtil.processHashVariable(s, workflowAssignment, null, null))
                .map(dataListService::fromJson)
                .orElseThrow(() -> new OdooUtilityException("DataList [" + datalistId + "] not found"));

        final Map<String, String[]> filters = getDataListFilter();
        getCollectFilters(dataList, filters);

        return dataList;
    }

    protected String getDataListId() {
        return getPropertyString("dataListId");
    }

    protected Map<String, String[]> getDataListFilter() {
        return Arrays.stream(getPropertyGrid("dataListFilter"))
                .collect(Collectors.toMap(m -> m.get("name"), m -> {
                    String values = m.get("value");
                    String[] split = values.split("[;,]");
                    return split;
                }));
    }

    protected void getCollectFilters(@Nonnull final DataList dataList, @Nonnull final Map<String, String[]> filters) {
        Optional.of(dataList)
                .map(DataList::getFilters)
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> Optional.of(f)
                        .map(DataListFilter::getName)
                        .map(filters::get)
                        .map(l -> l.length > 0)
                        .orElse(false))
                .forEach(f -> f.getType().setProperty("defaultValue", String.join(";", filters.get(f.getName()))));

        dataList.getFilterQueryObjects();
        dataList.setFilters(null);
    }

    protected Map<String, Object> remapToOdooRow(Map<String, Object> dataListRow) {
        Map<String, String> dataListToOdoofieldMapping = new HashMap<>();
        Map<String, Object> odooRow = new HashMap<>();

        dataListRow.forEach((key, value) -> odooRow.put(dataListToOdoofieldMapping.getOrDefault(key, key), value));
        return null;
    }
}
