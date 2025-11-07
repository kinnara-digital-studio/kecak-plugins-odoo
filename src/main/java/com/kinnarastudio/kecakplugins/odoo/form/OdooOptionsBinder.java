package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.CacheUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooDataListBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OdooOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder {
    final public static String LABEL = "Odoo Options Binder";

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
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final String valueField = getValueField();
        final String labelField = getLabelField();
        final String groupingField = getGroupingField();

        final Stream<SearchFilter> defaultFilterStream = Arrays.stream(OdooDataListBinderUtil.getFilter(this));
        final Stream<SearchFilter> filterQueryObjectStream = groupingField.isEmpty() ? Stream.empty() : Optional.ofNullable(dependencyValues)
                .stream()
                .flatMap(Arrays::stream)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> {
                    try {
                        return new SearchFilter(groupingField, Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        return new SearchFilter(groupingField, s);
                    }
                });

        final SearchFilter[] filters = Stream.concat(defaultFilterStream, filterQueryObjectStream)
                .toArray(SearchFilter[]::new);

        final String cacheKey = CacheUtil.getCacheKey(this.getClass(),
                database, user, model, valueField, labelField, groupingField, Arrays.stream(filters).map(SearchFilter::getValue).map(String::valueOf).collect(Collectors.joining()));

        final FormRowSet cached = (FormRowSet) CacheUtil.getCached(cacheKey);
        if (cached != null && cached.size() > 1) {
            LogUtil.debug(getClassName(), "Cache hit for key " + cacheKey);
            LogUtil.info(getClassName(), "Cache hit for key " + cacheKey);
            return cached;
        }

        try {
            final boolean hideEmptyValue = hideEmptyValue();

            FormRowSet ret = Arrays.stream(rpc.searchRead(model, filters, "id", null, null))
                    .map(m -> {
                        final String value = String.valueOf(m.get(valueField));
                        final String label = String.valueOf(m.get(labelField));
                        final String grouping = String.valueOf(m.get(groupingField));

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

            LogUtil.info(getClassName(), "Creating Cache for key " + cacheKey);

            return CacheUtil.putCache(cacheKey, ret);

        } catch (OdooCallMethodException e) {
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
}
