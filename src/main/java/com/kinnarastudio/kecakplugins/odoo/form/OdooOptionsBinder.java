package com.kinnarastudio.kecakplugins.odoo.form;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.kinnarastudio.kecakplugins.odoo.app.webservice.OdooTestConnectionWebService;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.DataType;
import com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import com.kinnarastudio.odooxmlrpc.rpc.OdooRpc;
import org.apache.commons.lang3.tuple.Pair;
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
import org.json.JSONException;
import org.json.JSONObject;

public class OdooOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder {
    public static final String LABEL = "Odoo Options Binder";
    final private Pattern fieldPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)(?:\\[(\\d+)])?(?!\\w)");

    final private Predicate<String> isEmpty = String::isEmpty;
    final private Predicate<String> isNotEmpty = isEmpty.negate();

    @Override
    public boolean useAjax() {
        return true;
    }

    @Override
    public FormRowSet loadAjaxOptions(@Nullable String[] dependencyValues) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);

        final String valueField = getValueField();
        final String labelField = getLabelField();
        final String groupingField = getGroupingField();

        String groupingBaseField;
        Integer groupingIndex;

        if (!groupingField.isEmpty()) {
            Matcher mGrouping = fieldPattern.matcher(groupingField);
            if (mGrouping.find()) {
                groupingBaseField = mGrouping.group(1);
                groupingIndex = mGrouping.group(2) == null ? null : Integer.parseInt(mGrouping.group(2));
            } else {
                groupingIndex = null;
                groupingBaseField = groupingField;
            }
        } else {
            groupingBaseField = "";
            groupingIndex = null;
        }

        final Stream<SearchFilter> defaultFilterStream = Arrays.stream(OdooDataListBinderUtil.getFilter(this));
        final Object[] dependencyFilter = Optional.ofNullable(dependencyValues)
                .stream()
                .flatMap(Arrays::stream)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return s;
                    }
                }).toArray(Object[]::new);
        final Stream<SearchFilter> filterQueryObjectStream = groupingField.isEmpty() ? Stream.empty() : Stream.of(new SearchFilter(groupingBaseField, SearchFilter.Operator.IN, dependencyFilter));

        final SearchFilter[] filters = Stream.concat(defaultFilterStream, filterQueryObjectStream)
                .toArray(SearchFilter[]::new);

        final String cacheKey = CacheUtil.getCacheKey(this.getClass(),
                database, user, model, valueField, labelField, groupingField, Arrays.stream(filters).map(SearchFilter::getValue).map(String::valueOf).collect(Collectors.joining()));

        final FormRowSet cached = (FormRowSet) CacheUtil.getCached(cacheKey);
        if (cached != null && cached.size() > 1) {
            LogUtil.debug(getClassName(), "Cache hit for key " + cacheKey);
            return cached;
        }

        try {
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

            final boolean hideEmptyValue = hideEmptyValue();

            Set<String> fieldsSet = new HashSet<>();
//            Matcher mValue = fieldPattern.matcher(valueField);
//            fieldsSet.add(mValue.find() ? mValue.group(1) : valueField);
            fieldsSet.add(valueField);
            Matcher mFields = fieldPattern.matcher(labelField);
            while (mFields.find()) {
                fieldsSet.add(mFields.group(1));
            }
            if (!groupingField.isEmpty()) {
                fieldsSet.add(groupingBaseField);
            }
            String[] requiredFields = fieldsSet.toArray(new String[0]);
//            LogUtil.info(getClassName(), "search_read model=[" + model + "] fields=" + Arrays.toString(requiredFields)
//                    + " valueField=[" + valueField + "] labelField=[" + labelField + "] groupingField=[" + groupingField + "]");

            // Safety limit to optimize cross-network XML-RPC payload parsing sizes
            FormRowSet ret = Arrays.stream(rpc.searchRead(model, requiredFields, filters, "id", null, null))
                    .map(m -> {
                        final String value = String.valueOf(m.get(valueField));

                        Matcher matcher = fieldPattern.matcher(labelField);
                        StringBuilder labelBuffer = new StringBuilder();

                        while (matcher.find()) {
                            String field = matcher.group(1);
                            Integer labelIndex = matcher.group(2) != null ? null : Integer.parseInt(matcher.group(2));
                            String fieldValue = extractIndexedValue(m, field, labelIndex);
                            matcher.appendReplacement(labelBuffer, Matcher.quoteReplacement(fieldValue));
                        }
                        matcher.appendTail(labelBuffer);
                        final String label = labelBuffer.toString();
                        final String grouping = groupingField.isEmpty() ? "" : extractIndexedValue(m, groupingBaseField, groupingIndex);

                        if (hideEmptyValue && value.isEmpty())
                            return null;

                        if (getPropertyString("showLabelOnly").equalsIgnoreCase("true")) {
                            return new FormRow() {{
                                setProperty(FormUtil.PROPERTY_VALUE, value);
                                setProperty(FormUtil.PROPERTY_LABEL, label);
                                setProperty(FormUtil.PROPERTY_GROUPING, grouping);
                            }};
                        } else {
                            return new FormRow() {{
                                setProperty(FormUtil.PROPERTY_VALUE, value);
                                setProperty(FormUtil.PROPERTY_LABEL, label + " (" + value + ")");
                                setProperty(FormUtil.PROPERTY_GROUPING, grouping);
                            }};
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(() -> new FormRowSet() {{
                        setMultiRow(true);
                        if (!hideEmptyValue) {
                            add(new FormRow() {{
                                setProperty(FormUtil.PROPERTY_VALUE, "");
                                setProperty(FormUtil.PROPERTY_LABEL, getEmptyLabel());
                            }});
                        }
                    }}));

            LogUtil.debug(getClassName(), "Creating Cache for key " + cacheKey);
            return CacheUtil.putCache(cacheKey, ret);

        } catch (OdooAuthorizationException |
                 com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
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
        final Object[] argsAuth = new Object[]{OdooTestConnectionWebService.class.getName()};

        final JSONArray jsonDataTypes = Arrays.stream(DataType.values())
                .map(Enum::name)
                .map(s -> new JSONObject() {{
                    try {
                        put(FormUtil.PROPERTY_VALUE, s);
                        put(FormUtil.PROPERTY_LABEL, s);
                    } catch (JSONException ignored) {
                    }
                }})
                .collect(JSONCollectors.toJSONArray());

        final Object[] argsBinder = new Object[]{
                jsonDataTypes.toString(),
        };

        final Pair<String, Object[]>[] resources = new Pair[]{
                Pair.of("/properties/common/OdooAuthorization.json", argsAuth),
                Pair.of("/properties/form/OdooOptionsBinder.json", argsBinder)
        };

        return Arrays.stream(resources)
                .map(pair -> AppUtil.readPluginResource(getClassName(), pair.getLeft(), pair.getRight(), true, ""))
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
    private String extractIndexedValue(Map<String, Object> record, String fieldName, Integer index) {
        Object rawValue = record.get(fieldName);
        if (index != null && rawValue instanceof Object[]) {
            Object[] arr = (Object[]) rawValue;
            return (index >= 0 && index < arr.length) ? String.valueOf(arr[index]) : "";
        }
        return formatOdooValue(rawValue);
    }
}
