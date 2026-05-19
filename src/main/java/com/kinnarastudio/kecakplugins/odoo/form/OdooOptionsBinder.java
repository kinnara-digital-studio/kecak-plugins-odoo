package com.kinnarastudio.kecakplugins.odoo.form;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormAjaxOptionsBinder;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadOptionsBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.CacheUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooDataListBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;

public class OdooOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder {
    public static final String LABEL = "Odoo Options Binder";

    final private Predicate<String> isEmpty = String::isEmpty;
    final private Predicate<String> isNotEmpty = isEmpty.negate();

    @Override
    public boolean useAjax() {
        return true;
    }

//    @Override
//    public FormRowSet loadAjaxOptions(@Nullable String[] dependencyValues) {
//        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
//        final String database = OdooAuthorizationUtil.getDatabase(this);
//        final String user = OdooAuthorizationUtil.getUsername(this);
//        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
//        final String model = OdooAuthorizationUtil.getModel(this);
//        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);
//
//        final String valueField = getValueField();
//        final String labelField = getLabelField();
//        final String groupingField = getGroupingField();
//
//        final Stream<SearchFilter> defaultFilterStream = Arrays.stream(OdooDataListBinderUtil.getFilter(this));
//        final Stream<SearchFilter> filterQueryObjectStream = groupingField.isEmpty()
//                ? Stream.empty() : Optional.ofNullable(dependencyValues)
//                       .filter(arr -> arr.length > 0)
//                       .map(arr -> {
//                           Integer[] intIds = Arrays.stream(arr)
//                               .filter(Predicate.not(String::isEmpty))
//                               .map(s -> {
//                                   try { return Integer.parseInt(s); }
//                                   catch (NumberFormatException e) { return null; }
//                               })
//                               .filter(Objects::nonNull)
//                               .toArray(Integer[]::new);
//
//                           if (intIds.length == arr.length) {
//                               return new SearchFilter(groupingField, SearchFilter.IN, (Object) intIds);
//                           }
//
//                           String[] strIds = Arrays.stream(arr)
//                                   .filter(Predicate.not(String::isEmpty))
//                                   .toArray(String[]::new);
//                           return new SearchFilter(groupingField, SearchFilter.IN, (Object) strIds);
//                       }).stream();
//
//        // Filter valueField in [ids] — NEW: kalau groupingField kosong, filter by valueField
//        final Stream<SearchFilter> valueFilterStream = !groupingField.isEmpty() ? Stream.empty()
//                : Optional.ofNullable(dependencyValues)
//                  .filter(arr -> arr.length > 0)
//                  .map(arr -> {
//                      Integer[] intIds = Arrays.stream(arr)
//                              .map(s -> {
//                                  try { return Integer.parseInt(s); }
//                                  catch (NumberFormatException e) { return null; }
//                              })
//                              .filter(Objects::nonNull)
//                              .toArray(Integer[]::new);
//                      return intIds.length > 0
//                             ? new SearchFilter(valueField, SearchFilter.IN, (Object) intIds)
//                             : null;
//                  })
//                  .filter(Objects::nonNull)
//                  .stream();
//
//        final SearchFilter[] filters = Stream.concat(
//                        Stream.concat(defaultFilterStream, filterQueryObjectStream),
//                        valueFilterStream)
//                .toArray(SearchFilter[]::new);
//
//        final String cacheKey = CacheUtil.getCacheKey(this.getClass(),
//                database, user, model, valueField, labelField, groupingField,
//                Arrays.stream(filters)
//                        .map(SearchFilter::getValue)
//                        .map(v -> {
//                            if (v instanceof Object[]) return Arrays.stream((Object[]) v).map(String::valueOf).collect(Collectors.joining("_"));
//                            if (v instanceof int[]) return Arrays.toString((int[]) v);
//                            return String.valueOf(v);
//                        })
//                        .collect(Collectors.joining()));
//
//        final FormRowSet cached = (FormRowSet) CacheUtil.getCached(cacheKey);
//        if (cached != null && cached.size() > 1) {
//            LogUtil.debug(getClassName(), "Cache hit for key " + cacheKey);
//            return cached;
//        }
//
////        LogUtil.info(getClassName(), labelFieldStream.toString());
////        LogUtil.info(getClassName(), groupingFieldStream.toString());
////
////        LogUtil.info(getClassName(), Arrays.toString(requiredFields));
//
//        try {
//            final boolean hideEmptyValue = hideEmptyValue();
//
//            final Pattern fieldPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)\\b");
//
//            Stream<String> labelFieldStream = fieldPattern.matcher(labelField)
//                    .results()
//                    .map(mr -> mr.group(1));
//
//            Stream<String> groupingFieldStream = fieldPattern.matcher(groupingField)
//                    .results()
//                    .map(mr -> mr.group(1));
//
//            String[] requiredFields = Stream.of(Stream.of(valueField), labelFieldStream, groupingFieldStream)
//                    .flatMap(s -> s)
//                    .filter(f -> f != null && !f.isEmpty())
//                    .distinct()
//                    .toArray(String[]::new);
//
//            FormRowSet ret = Arrays.stream(rpc.searchRead(model, requiredFields, filters, "id", null, null))
//                    .map(m -> {
//                        final String value = String.valueOf(m.get(valueField));
//
//                        Matcher matcher = fieldPattern.matcher(labelField);
//                        StringBuffer labelBuffer = new StringBuffer();
//
//                        while (matcher.find()) {
//                            String fieldName = matcher.group(1);
//                            String fieldValue = formatOdooValue(m.get(fieldName));
//                            matcher.appendReplacement(labelBuffer, Matcher.quoteReplacement(fieldValue));
//                        }
//                        matcher.appendTail(labelBuffer);
//                        final String label = labelBuffer.toString();
//
//                        // final String label = formatOdooValue(m.get(labelField));
//                        final String grouping = String.valueOf(m.get(groupingField));
//
//                        if (hideEmptyValue && value.isEmpty())
//                            return null;
//
//                        if (getPropertyString("showLabelOnly").equalsIgnoreCase("true")) {
//                            return new FormRow() {{
//                                setProperty(FormUtil.PROPERTY_VALUE, value);
//                                setProperty(FormUtil.PROPERTY_LABEL, label);
//                                setProperty(FormUtil.PROPERTY_GROUPING, grouping);
//                            }};
//                        } else {
//                            return new FormRow() {{
//                                setProperty(FormUtil.PROPERTY_VALUE, value);
//                                setProperty(FormUtil.PROPERTY_LABEL, label + " (" + value + ")");
//                                setProperty(FormUtil.PROPERTY_GROUPING, grouping);
//                            }};
//                        }
//                    })
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toCollection(() -> new FormRowSet() {{
//                        setMultiRow(true);
//                        if (!hideEmptyValue) {
//                            add(new FormRow() {{
//                                setProperty(FormUtil.PROPERTY_VALUE, "");
//                                setProperty(FormUtil.PROPERTY_LABEL, getEmptyLabel());
//                            }});
//                        }
//                    }}));
//
//            LogUtil.debug(getClassName(), "Creating Cache for key " + cacheKey);
//            return CacheUtil.putCache(cacheKey, ret);
//
//        } catch (OdooCallMethodException e) {
//            LogUtil.error(getClass().getName(), e, e.getMessage());
//            return null;
//        }
//
//    }

//@Override
//public FormRowSet loadAjaxOptions(@Nullable String[] dependencyValues) {
//    final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
//    final String database = OdooAuthorizationUtil.getDatabase(this);
//    final String user = OdooAuthorizationUtil.getUsername(this);
//    final String apiKey = OdooAuthorizationUtil.getApiKey(this);
//    final String model = OdooAuthorizationUtil.getModel(this);
//    final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);
//
//    final String valueField = getValueField();
//    final String labelField = getLabelField();
//    final String groupingField = getGroupingField();
//
//    // 1. Build Filters carefully to prevent database hangs
//    final List<SearchFilter> filtersList = new ArrayList<>(Arrays.asList(OdooDataListBinderUtil.getFilter(this)));
//
//    if (dependencyValues != null && dependencyValues.length > 0) {
//        // Filter out empty strings to prevent malformed Odoo domains
//        Object[] processedValues = Arrays.stream(dependencyValues)
//                .filter(s -> s != null && !s.isEmpty())
//                .map(s -> {
//                    try { return Integer.parseInt(s); }
//                    catch (NumberFormatException e) { return s; }
//                })
//                .toArray();
//
//        if (processedValues.length > 0) {
//            // If grouping field is empty, filter by the primary value field (common for AJAX dependencies)
//            String targetField = groupingField.isEmpty() ? valueField : groupingField;
//            filtersList.add(new SearchFilter(targetField, SearchFilter.IN, (Object) processedValues));
//        }
//    }
//
//    final SearchFilter[] filters = filtersList.toArray(new SearchFilter[0]);
//
//    // 2. Generate a robust Cache Key
//    // We stringify the filter values specifically to ensure the cache is accurate
//    final String filterKeyPart = Arrays.stream(filters)
//            .map(f -> String.valueOf(f.getValue()))
//            .collect(Collectors.joining("_"));
//    final String cacheKey = CacheUtil.getCacheKey(this.getClass(), database, model, valueField, labelField, groupingField, filterKeyPart);
//
//    final FormRowSet cached = (FormRowSet) CacheUtil.getCached(cacheKey);
//    if (cached != null) {
//        LogUtil.debug(getClassName(), "Cache hit: " + cacheKey);
//        return cached;
//    }
//
//    try {
//        final boolean hideEmptyValue = hideEmptyValue();
//        final Pattern fieldPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)\\b");
//
//        // 3. Define only the fields we actually need (Performance Improvement)
//        Set<String> fieldsSet = new HashSet<>();
//        fieldsSet.add(valueField);
//        if (groupingField != null && !groupingField.isEmpty()) fieldsSet.add(groupingField);
//
//        Matcher mFields = fieldPattern.matcher(labelField);
//        while (mFields.find()) {
//            fieldsSet.add(mFields.group(1));
//        }
//        String[] requiredFields = fieldsSet.toArray(new String[0]);
//
//        // 4. FIXED RPC CALL: Order is (model, fields, filters, sort, offset, limit)
//        Object[] results = rpc.searchRead(model, requiredFields, filters, "id", null, null);
//
//        // Use a Set to track IDs we have already added to the result
//        final Set<String> seenIds = new HashSet<>();
//
//        FormRowSet ret = Arrays.stream(results)
//                .map(obj -> (java.util.Map<String, Object>) obj)
//                .map(m -> {
//                    final String value = String.valueOf(m.getOrDefault(valueField, ""));
//                    // --- DEDUPLICATION CHECK ---
//                    // If we've already seen this ID, return null so it gets filtered out
//                    if (value.isEmpty() || seenIds.contains(value)) {
//                        return null;
//                    }
//                    seenIds.add(value);
//                    // ----------------------------
//
//                    Matcher matcher = fieldPattern.matcher(labelField);
//                    StringBuffer labelBuffer = new StringBuffer();
//                    while (matcher.find()) {
//                        String fieldName = matcher.group(1);
//                        String fieldValue = formatOdooValue(m.get(fieldName));
//                        matcher.appendReplacement(labelBuffer, Matcher.quoteReplacement(fieldValue));
//                    }
//                    matcher.appendTail(labelBuffer);
//
//                    final String label = labelBuffer.toString();
//                    final String grouping = String.valueOf(m.getOrDefault(groupingField, ""));
//
//                    if (hideEmptyValue && (value == null || value.isEmpty())) return null;
//
//                    FormRow row = new FormRow();
//                    row.setProperty(FormUtil.PROPERTY_VALUE, value);
//                    row.setProperty(FormUtil.PROPERTY_LABEL, getPropertyString("showLabelOnly").equalsIgnoreCase("true") ? label : label + " (" + value + ")");
//                    row.setProperty(FormUtil.PROPERTY_GROUPING, grouping);
//                    // Set the ID explicitly to ensure Joget doesn't generate a random UUID
//                    row.setId(value);
//
//                    return row;
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toCollection(() -> {
//                    FormRowSet frs = new FormRowSet();
//                    frs.setMultiRow(true);
//                    if (!hideEmptyValue) {
//                        FormRow emptyRow = new FormRow();
//                        emptyRow.setProperty(FormUtil.PROPERTY_VALUE, "");
//                        emptyRow.setProperty(FormUtil.PROPERTY_LABEL, getEmptyLabel());
//                        frs.add(emptyRow);
//                    }
//                    return frs;
//                }));
//
//        LogUtil.debug(getClassName(), "Creating Cache: " + cacheKey);
//        return CacheUtil.putCache(cacheKey, ret);
//
//    } catch (OdooCallMethodException e) {
//        LogUtil.error(getClassName(), e, "Odoo Connection Error: " + e.getMessage());
//        return null;
//    }
//}

@Override
public FormRowSet loadAjaxOptions(@Nullable String[] dependencyValues) {
    final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
    final String database = OdooAuthorizationUtil.getDatabase(this);
    final String user = OdooAuthorizationUtil.getUsername(this);
    final String apiKey = OdooAuthorizationUtil.getApiKey(this);
    final String model = OdooAuthorizationUtil.getModel(this);
    final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

    final String valueField = getValueField();
    final String labelField = getLabelField();
    final String groupingField = getGroupingField();

    // 1. Sanitize and Prepare Dependency Filters
    final List<SearchFilter> filtersList = new ArrayList<>(Arrays.asList(OdooDataListBinderUtil.getFilter(this)));

    // Track dependencies for the Cache Key
    String dependencyKeyPart = "none";

    if (dependencyValues != null && dependencyValues.length > 0) {
        Object[] sanitizedIds = Arrays.stream(dependencyValues)
                .filter(s -> s != null && !s.isEmpty() && !"null".equalsIgnoreCase(s))
                .map(s -> {
                    try { return Integer.parseInt(s); }
                    catch (NumberFormatException e) { return s; }
                })
                .toArray();

        if (sanitizedIds.length > 0) {
            dependencyKeyPart = Arrays.toString(sanitizedIds);
            String targetField = groupingField.isEmpty() ? valueField : groupingField;
            filtersList.add(new SearchFilter(targetField, SearchFilter.IN, sanitizedIds));
        } else {
            // If dependencies are provided but all are empty/invalid, return empty to prevent full table scan
            return new FormRowSet();
        }
    }

    // 2. Generate Cache Key (Prevent serving stale/duplicate data from memory)
    final String cacheKey = CacheUtil.getCacheKey(this.getClass(), database, model, valueField, labelField, groupingField, dependencyKeyPart);
    final FormRowSet cached = (FormRowSet) CacheUtil.getCached(cacheKey);
    if (cached != null) return cached;

    try {
        final boolean hideEmptyValue = hideEmptyValue();
        final Pattern fieldPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)\\b");

        // 3. Collect only the required fields for the Odoo Query
        Set<String> fieldsSet = new HashSet<>();
        fieldsSet.add(valueField);
        if (!groupingField.isEmpty()) fieldsSet.add(groupingField);
        Matcher mFields = fieldPattern.matcher(labelField);
        while (mFields.find()) {
            fieldsSet.add(mFields.group(1));
        }
        String[] requiredFields = fieldsSet.toArray(new String[0]);

        // 4. EXECUTE RPC CALL
        // Signature: searchRead(String model, String[] fields, SearchFilter[] filters, String sort, Integer offset, Integer limit)
        // Note: We add a limit of 1000 as a safety valve against database locking on massive datasets.
        Object[] results = rpc.searchRead(model, requiredFields, filtersList.toArray(new SearchFilter[0]), "id", 0, 1000);

        if (results == null) return new FormRowSet();

        // 5. DEDUPLICATION LOGIC (Crucial for fixing the SQL Integrity Error)
        final Set<String> seenValues = new HashSet<>();
        final FormRowSet ret = new FormRowSet();
        ret.setMultiRow(true);

        // Add empty option if configured
        if (!hideEmptyValue) {
            FormRow emptyRow = new FormRow();
            emptyRow.setProperty(FormUtil.PROPERTY_VALUE, "");
            emptyRow.setProperty(FormUtil.PROPERTY_LABEL, getEmptyLabel());
            ret.add(emptyRow);
        }

        for (Object obj : results) {
            Map<String, Object> record = (Map<String, Object>) obj;
            String value = String.valueOf(record.getOrDefault(valueField, ""));

            // If this ID is blank or we have already added it, skip it!
            if (value.isEmpty() || seenValues.contains(value)) {
                continue;
            }
            seenValues.add(value);

            // Process Label Replacement
            Matcher matcher = fieldPattern.matcher(labelField);
            StringBuffer labelBuffer = new StringBuffer();
            while (matcher.find()) {
                String fieldName = matcher.group(1);
                String fieldValue = formatOdooValue(record.get(fieldName));
                matcher.appendReplacement(labelBuffer, Matcher.quoteReplacement(fieldValue));
            }
            matcher.appendTail(labelBuffer);

            final String label = labelBuffer.toString();
            final String grouping = String.valueOf(record.getOrDefault(groupingField, ""));

            // Construct the FormRow
            FormRow row = new FormRow();
            row.setId(value); // Set the Primary Key explicitly to the Odoo value
            row.setProperty(FormUtil.PROPERTY_VALUE, value);

            boolean showLabelOnly = "true".equalsIgnoreCase(getPropertyString("showLabelOnly"));
            row.setProperty(FormUtil.PROPERTY_LABEL, showLabelOnly ? label : label + " (" + value + ")");
            row.setProperty(FormUtil.PROPERTY_GROUPING, grouping);

            ret.add(row);
        }

        LogUtil.debug(getClassName(), "Created Cache for key " + cacheKey + " with " + ret.size() + " unique rows.");
        return CacheUtil.putCache(cacheKey, ret);

    } catch (Exception e) {
        LogUtil.error(getClassName(), e, "Error loading Odoo options: " + e.getMessage());
        return new FormRowSet();
    }
}

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        setFormData(formData);
        return loadAjaxOptions(null);
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
        final String[] resources = new String[]{
                "/properties/common/OdooAuthorization.json",
                "/properties/form/OdooOptionsBinder.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    protected String getValueField() {
        return Optional.of(getPropertyString("valueField"))
                .filter(isNotEmpty)
                .orElse("id");
    }

    protected String getLabelField() {
        return getPropertyString("labelField");
    }

    protected String getGroupingField() {
        return getPropertyString("groupingField");
    }

    protected String getEmptyLabel() {
        return getPropertyString("emptyLabel");
    }

    protected boolean hideEmptyValue() {
        return "true".equalsIgnoreCase(getPropertyString("hideEmptyValue"));
    }

    protected String getFormattingType() {
        return getPropertyString("formattingType");
    }

    protected boolean showId() {
        return "showId".equalsIgnoreCase(getFormattingType());
    }

    protected boolean isAsOptions() {
        return "asOptions".equalsIgnoreCase(getFormattingType());
    }

    protected String formatOdooValue(Object value) {
        if (value instanceof Object[]) {
            final Object[] values = (Object[]) value;
            if (showId()) {
                return Arrays.stream(values)
                        .findFirst()
                        .map(String::valueOf)
                        .orElse("");
            } else if (isAsOptions()) {
                return Arrays.stream(values).skip(1)
                        .findFirst()
                        .map(String::valueOf)
                        .orElse("");
            } else {
                return Arrays.stream(values)
                        .map(String::valueOf)
                        .collect(Collectors.joining(";"));
            }
        }
        return value == null ? "" : String.valueOf(value);
    }
}
