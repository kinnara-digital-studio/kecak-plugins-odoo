package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;

public class OdooManyToOneLookupFormatter extends DataListColumnFormatDefault {
    public static final String LABEL = "Odoo Many2one Lookup Formatter";

    /**
     * Cache keyed by "dataListId_columnName_targetModel_targetField".
     * Populated once per DataList render, reused for all rows.
     */
    private static final Map<String, Map<Integer, String>> LOOKUP_CACHE = new ConcurrentHashMap<>();

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        if (value == null)
            return "";

        final String targetModel = getPropertyString("targetModel");
        final String targetField = getPropertyString("targetField");
        final String columnName = column.getName();

        // Debug: log value type and content
//        LogUtil.info(getClassName(),
//                "format() column=[" + columnName + "] valueType=[" + value.getClass().getSimpleName()
//                        + "] value=[" + describeValue(value) + "]"
//                        + " targetModel=[" + targetModel + "] targetField=[" + targetField + "]");

        if (targetModel.isEmpty() || targetField.isEmpty()) {
            return formatRawValue(value);
        }

        // Extract ID from Many2one value: [id, "name"] or just id
        final Integer id = extractId(value);
//        LogUtil.info(getClassName(),
//                "extractId() column=[" + columnName + "] → id=[" + id + "]");

        if (id == null) {
            return formatRawValue(value);
        }

        // Build cache key unique to this DataList + column + config
        final String cacheKey = buildCacheKey(dataList, column, targetModel, targetField);

        // Get or build the lookup map (ONE Odoo call for ALL rows)
        Map<Integer, String> lookupMap = LOOKUP_CACHE.get(cacheKey);
        if (lookupMap == null) {
            synchronized (LOOKUP_CACHE) {
                lookupMap = LOOKUP_CACHE.get(cacheKey);
                if (lookupMap == null) {
                    lookupMap = buildLookupMap(dataList, column, targetModel, targetField);
                    LOOKUP_CACHE.put(cacheKey, lookupMap);
                }
            }
        }

        // Return the looked-up value, or fallback to the display name
        String result = lookupMap.get(id);
//        LogUtil.info(getClassName(),
//                "lookup result: id=[" + id + "] → value=[" + result + "]");

        if (result != null && !result.isEmpty()) {
            return result;
        }

        // Fallback: return the display name from Many2one
        return formatRawValue(value);
    }

    /**
     * Build the lookup map by:
     * 1. Collecting all unique Many2one IDs from the DataList's data
     * 2. Doing ONE batch searchRead on the target model
     */
    private Map<Integer, String> buildLookupMap(DataList dataList, DataListColumn column,
            String targetModel, String targetField) {
        final String columnName = column.getName();

        // Step 1: Collect all unique IDs from the DataList rows
        final Set<Integer> ids;
        try {
            @SuppressWarnings("unchecked")
            java.util.Collection<Object> rows = dataList.getRows();
            ids = rows.stream()
                    .map(row -> {
                        Object val;
                        if (row instanceof Map) {
                            val = ((Map<?, ?>) row).get(columnName);
                        } else {
                            return null;
                        }
                        return extractId(val);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "Failed to collect IDs from DataList: " + e.getMessage());
            return new HashMap<>();
        }

        if (ids.isEmpty()) {
            LogUtil.debug(getClassName(), "No IDs to look up for column [" + columnName + "]");
            return new HashMap<>();
        }

//        LogUtil.info(getClassName(),
//                "Batch lookup: [" + targetField + "] from [" + targetModel + "] for "
//                        + ids.size() + " unique IDs in column [" + columnName + "]");

        // Step 2: ONE batch searchRead
        try {
            final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
            final String database = OdooAuthorizationUtil.getDatabase(this);
            final String user = OdooAuthorizationUtil.getUsername(this);
            final String apiKey = OdooAuthorizationUtil.getApiKey(this);
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

            final SearchFilter inFilter = new SearchFilter("id", SearchFilter.IN, ids.toArray(new Integer[0]));

            return Arrays.stream(
                    rpc.searchRead(targetModel, new SearchFilter[] { inFilter }, null, null, null))
                    .collect(Collectors.toMap(
                            m -> (Integer) m.get("id"),
                            m -> Optional.ofNullable(m.get(targetField))
                                    .map(String::valueOf)
                                    .filter(s -> !"null".equals(s) && !s.isBlank())
                                    .orElse(""),
                            (a, b) -> a));

        } catch (OdooCallMethodException e) {
            LogUtil.error(getClassName(), e,
                    "Batch lookup failed for [" + targetModel + "." + targetField + "]: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Extract the integer ID from a Many2one value.
     * Many2one can be: Object[]{id, "name"}, Integer, or String of an integer.
     */
    private Integer extractId(Object value) {
        if (value == null)
            return null;

        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= 1 && arr[0] instanceof Integer) {
                return (Integer) arr[0];
            }
            if (arr.length >= 1) {
                try {
                    return Integer.parseInt(String.valueOf(arr[0]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Format the raw Many2one value for display (fallback).
     * [id, "name"] → "name", other → toString
     */
    private String formatRawValue(Object value) {
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= 2)
                return String.valueOf(arr[1]);
            if (arr.length == 1)
                return String.valueOf(arr[0]);
            return "";
        }
        return String.valueOf(value);
    }

    /**
     * Describe a value for debug logging.
     */
    private String describeValue(Object value) {
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            StringBuilder sb = new StringBuilder("Object[");
            sb.append(arr.length).append("]{");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(arr[i] == null ? "null" : arr[i].getClass().getSimpleName() + ":" + arr[i]);
            }
            sb.append("}");
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private String buildCacheKey(DataList dataList, DataListColumn column,
            String targetModel, String targetField) {
        return String.join("_",
                dataList.getId() != null ? dataList.getId() : "dl",
                column.getName(),
                targetModel,
                targetField);
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
        final String[] resources = new String[] {
                "/properties/common/OdooAuthorization.json",
                "/properties/datalist/formatter/OdooManyToOneLookupFormatter.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }
}
