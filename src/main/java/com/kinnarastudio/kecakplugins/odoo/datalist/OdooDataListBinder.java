package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooDataListBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.IOdooFilter;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Odoo DataList Binder
 */
public class OdooDataListBinder extends DataListBinderDefault {
    public final static String LABEL = "Odoo DataList Binder";

    public final static Collection<String> VALID_COLUMN_TYPES = new HashSet<>() {{
        add("string");
        add("date");
        add("datetime");
        add("integer");
        add("char");
        add("monetery");
        add("boolean");
    }};

    @Override
    public DataListColumn[] getColumns() {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        try {
            return rpc.fieldsGet(model).entrySet().stream()
//                    .filter(e -> {
//                        final Map<String, Object> metadata = e.getValue();
//                        final String type = (String) metadata.get("type");
//                        return VALID_COLUMN_TYPES.contains(type);
//                    })

                    .map(e -> {
                        final String field = e.getKey();
                        final Map<String, Object> metadata = e.getValue();

                        final String label = (String) metadata.getOrDefault("string", field);

                        final boolean sortable = "true".equals(metadata.getOrDefault("sortable", true));

                        return new DataListColumn(field, label, sortable);
                    })
                    .toArray(DataListColumn[]::new);

        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return new DataListColumn[0];
        }
    }

    @Override
    public String getPrimaryKeyColumnName() {
        final String value = getPropertyString("primaryKeyColumn");
        return value.isEmpty() ? "id" : value;
    }

    @Override
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final SearchFilter[] filters = getFilters(filterQueryObjects);

        try {
            final String order = sort == null ? null : String.join(" ", sort, desc != null && desc ? "desc" : "");
            return Arrays.stream(rpc.searchRead(model, filters, order, start, rows))
                    .collect(Collectors.toCollection(DataListCollection<Map>::new));

        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final SearchFilter[] filters = getFilters(filterQueryObjects);

        try {
            return rpc.searchCount(model, filters);
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return 0;
        }
    }

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
                "/properties/datalist/OdooDataListBinder.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    protected SearchFilter[] getFilters(DataListFilterQueryObject[] filterQueryObjects) {
        final Stream<SearchFilter> defaultFilterStream = Arrays.stream(OdooDataListBinderUtil.getFilter(this));

        final Stream<SearchFilter> filterQueryObjectStream = Optional.ofNullable(filterQueryObjects)
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f instanceof IOdooFilter)
                .map(f -> (IOdooFilter) f)
                .filter(f -> f.getValue() != null && !f.getValue().isEmpty())
                .map(f -> {
                    final Object value = f.getDataType() == IOdooFilter.DataType.INTEGER ? Integer.parseInt(f.getValue()) : f.getValue();
                    return new SearchFilter(f.getField(), f.getOperator(), value);
                });

        return Stream.concat(defaultFilterStream, filterQueryObjectStream)
                .toArray(SearchFilter[]::new);
    }
}
