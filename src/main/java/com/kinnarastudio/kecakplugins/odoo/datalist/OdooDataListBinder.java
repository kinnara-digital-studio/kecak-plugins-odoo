package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.app.webservice.OdooTestConnectionWebService;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooDataListBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.IOdooFilter;
import com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException;
import com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException;
import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import com.kinnarastudio.odooxmlrpc.rpc.OdooRpc;
import org.apache.commons.lang3.tuple.Pair;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Odoo DataList Binder
 */
public class OdooDataListBinder extends DataListBinderDefault {
    public final static String LABEL = "Odoo DataList Binder";

    public final static Collection<String> VALID_COLUMN_TYPES = new HashSet<>() {{
        add(DataType.STRING.name());
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

        try {
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);
            return rpc.fieldsGet(model).stream()
                    .map(e -> {
                        final String field = e.getKey();
                        final String label = e.getString();
                        final boolean sortable = e.isSortable();
                        return new DataListColumn(field, label, sortable);
                    })
                    .toArray(DataListColumn[]::new);

        } catch (OdooAuthorizationException |
                 com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException e) {
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
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects,
                                      String sort, Boolean desc, Integer start, Integer rows) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);

        final SearchFilter[] filters = getFilters(filterQueryObjects);

        final String primaryKey = getPrimaryKeyColumnName();
        final String[] fields = Optional.ofNullable(dataList.getColumns())
                .map(cols -> Arrays.stream(cols)
                        .map(DataListColumn::getName)
                        .filter(name -> name != null && !name.isEmpty())
                        .distinct()
                        .toArray(String[]::new))
                .filter(arr -> arr.length > 0)
                .map(arr -> {
                    boolean hasPrimaryKey = Arrays.asList(arr).contains(primaryKey);
                    if (hasPrimaryKey) return arr;
                    return Stream.concat(Stream.of(primaryKey), Arrays.stream(arr))
                            .toArray(String[]::new);
                })
                .orElse(null);

        try {
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

            final String order = sort == null ? null : String.join(" ", sort, desc != null && desc ? "desc" : "");
            return Arrays.stream(rpc.searchRead(model, fields, filters, order, start, rows))
                    .collect(Collectors.toCollection(DataListCollection<Map>::new));

        } catch (OdooCallMethodException | OdooAuthorizationException e) {
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


        try {
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);
            final SearchFilter[] filters = getFilters(filterQueryObjects);
            return rpc.searchCount(model, filters);
        } catch (OdooCallMethodException | OdooAuthorizationException e) {
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
        return resourceBundle.getString("buildNumber");
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
        final Object[] authArgs = new Object[]{OdooTestConnectionWebService.class.getName()};

        final JSONArray jsonOperator = Arrays.stream(SearchFilter.Operator.values())
                .map(o -> new JSONObject() {{
                    try {
                        put(FormUtil.PROPERTY_VALUE, o.toString());
                        put(FormUtil.PROPERTY_LABEL, o.name());
                    } catch (JSONException ignored) {}
                }})
                .collect(JSONCollectors.toJSONArray());
        final JSONArray jsonJoin = Arrays.stream(SearchFilter.Join.values())
                .map(j -> new JSONObject() {{
                    try {
                        put(FormUtil.PROPERTY_VALUE, j.toString());
                        put(FormUtil.PROPERTY_LABEL, j.name());
                    } catch (JSONException ignored) {}
                }})
                .collect(JSONCollectors.toJSONArray());

        final JSONArray jsonDataTypes = Arrays.stream(DataType.values())
                .map(Enum::name)
                .map(s -> new JSONObject() {{
                    try {
                        put(FormUtil.PROPERTY_VALUE, s);
                        put(FormUtil.PROPERTY_LABEL, s);
                    } catch (JSONException ignored) {}
                }})
                .collect(JSONCollectors.toJSONArray());

        final Object[] binderArgs = new Object[]{
                jsonJoin.toString(),
                jsonOperator.toString(),
                jsonDataTypes.toString()
        };

        final Pair<String, Object[]>[] resources = new Pair[] {
                Pair.of("/properties/common/OdooAuthorization.json", authArgs),
                Pair.of("/properties/datalist/OdooDataListBinder.json", binderArgs)
        };

        return Arrays.stream(resources)
                .map(pair -> AppUtil.readPluginResource(getClassName(), pair.getLeft(), pair.getRight(), true, ""))
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
                .map(Try.onFunction(f -> {
                    DataType dataType = f.getDataType();

                    Object value;

                    if (SearchFilter.Operator.IN == f.getFilterOperator()) {
                        value = Arrays.stream(f.getValue().split(";"))
                                .map(String::trim)
                                .map(dataType::valueParser)
                                .filter(Objects::nonNull)
                                .toArray();
                    } else {
                        value = dataType.valueParser(f.getValue());
                    }

                    return new SearchFilter(f.getJoin(), f.getField(), f.getFilterOperator(), value);
                }, (NumberFormatException ignored) -> null))
                .filter(Objects::nonNull);

        return Stream.concat(defaultFilterStream, filterQueryObjectStream)
                .toArray(SearchFilter[]::new);
    }
}