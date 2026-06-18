package com.kinnarastudio.kecakplugins.odoo.form;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.kinnarastudio.kecakplugins.odoo.app.webservice.OdooTestConnectionWebService;
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

        final List<SearchFilter> filtersList = new ArrayList<>(Arrays.asList(OdooDataListBinderUtil.getFilter(this)));
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
                return new FormRowSet();
            }
        }

        // Generate a completely isolated unique memory key
        final String cacheKey = database + "_" + model + "_" + valueField + "_" + labelField + "_" + groupingField + "_" + dependencyKeyPart;

        // Capture configuration securely before handing off context to the background worker
        final boolean hideEmpty = hideEmptyValue();
        final String emptyLabelStr = getEmptyLabel();
        final String showLabelOnlyStr = getPropertyString("showLabelOnly");

        OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final Pattern fieldPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)\\b");

        return CacheUtil.getFromCache(cacheKey, () -> {
            try {
                Set<String> fieldsSet = new HashSet<>();
                fieldsSet.add(valueField);
                if (!groupingField.isEmpty()) fieldsSet.add(groupingField);
                Matcher mFields = fieldPattern.matcher(labelField);
                while (mFields.find()) {
                    fieldsSet.add(mFields.group(1));
                }
                String[] requiredFields = fieldsSet.toArray(new String[0]);

                // Safety limit to optimize cross-network XML-RPC payload parsing sizes
                Object[] results = rpc.searchRead(model, requiredFields, filtersList.toArray(new SearchFilter[0]), "id", null, null);

                final Set<String> seenValues = new HashSet<>();
                final FormRowSet ret = new FormRowSet();
                ret.setMultiRow(true);

                if (!hideEmpty) {
                    FormRow emptyRow = new FormRow();
                    emptyRow.setProperty(FormUtil.PROPERTY_VALUE, "");
                    emptyRow.setProperty(FormUtil.PROPERTY_LABEL, emptyLabelStr);
                    ret.add(emptyRow);
                }

                for (Object obj : results) {
                    Map<String, Object> record = (Map<String, Object>) obj;
                    String value = String.valueOf(record.getOrDefault(valueField, ""));
                    if (value.isEmpty() || seenValues.contains(value)) continue;
                    seenValues.add(value);

                    Matcher matcher = fieldPattern.matcher(labelField);
                    StringBuilder labelBuffer = new StringBuilder();
                    while (matcher.find()) {
                        String fieldName = matcher.group(1);
                        String fieldValue = formatOdooValue(record.get(fieldName));
                        matcher.appendReplacement(labelBuffer, Matcher.quoteReplacement(fieldValue));
                    }
                    matcher.appendTail(labelBuffer);

                    final String label = labelBuffer.toString();
                    final String grouping = String.valueOf(record.getOrDefault(groupingField, ""));

                    FormRow row = new FormRow();
                    row.setId(value);
                    row.setProperty(FormUtil.PROPERTY_VALUE, value);
                    row.setProperty(FormUtil.PROPERTY_LABEL, "true".equalsIgnoreCase(showLabelOnlyStr) ? label : label + " (" + value + ")");
                    row.setProperty(FormUtil.PROPERTY_GROUPING, grouping);
                    ret.add(row);
                }

                return ret;
            } catch (OdooCallMethodException e) {
                LogUtil.error(getClassName(), e, "Error loading Odoo options: " + e.getMessage());
                return null;
            }
        });
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
        final Object[] argsOdooAuth = new Object[]{OdooTestConnectionWebService.class.getName()};
        final Object[] argsOdooBinder = null;

        final Pair<String, Object[]>[] resources = new Pair[]{
                Pair.of("/properties/common/OdooAuthorization.json", argsOdooAuth),
                Pair.of("/properties/form/OdooOptionsBinder.json", argsOdooBinder)
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
}
