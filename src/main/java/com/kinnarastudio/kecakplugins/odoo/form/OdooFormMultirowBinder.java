package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooFormMultirowLoadBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Odoo Form MultiRow Binder
 */
public class OdooFormMultirowBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormLoadMultiRowElementBinder , FormStoreMultiRowElementBinder{
    public final static String LABEL = "Odoo Form Multirow Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if (primaryKey == null) return null;

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        try {
            final String foreignKeyField = OdooFormMultirowLoadBinderUtil.getForeignKeyField(this);
            final SearchFilter[] searchFilters = new SearchFilter[]{
                    new SearchFilter(foreignKeyField, Integer.parseInt(primaryKey))
            };

            return Arrays.stream(rpc.searchRead(model, searchFilters, null, null, null))
                    .map(m -> new FormRow() {{
                        m.forEach((k, v) -> {
                            if (v != null) setProperty(k, String.valueOf(v));
                        });
                    }})
                    .collect(Collectors.toCollection(() -> new FormRowSet() {{
                        setMultiRow(true);
                    }}));
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        if(rowSet == null) return null;

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final String foreignKeyField = OdooFormMultirowLoadBinderUtil.getForeignKeyField(this);

        final Form parentForm = FormUtil.findRootForm(element);
        final String recordId = parentForm.getPrimaryKeyValue(formData);

        final FormRowSet originalRowSet = Optional.ofNullable(load(element, recordId, formData))
                .orElseGet(FormRowSet::new);
        originalRowSet.sort(Comparator.comparing(FormRow::getId));

        final FormRowSet storedRowSet = rowSet.stream()
                .peek(r -> r.setProperty(foreignKeyField, recordId))
                .map(Try.onFunction(row -> {
                    final int foreignKey = Optional.ofNullable(row.getId())
                            .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> null))
                            .orElseGet(() -> Optional.ofNullable(formData.getPrimaryKeyValue())
                                    .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> null))
                                    .orElse(0));

                    final Map<String, Object> record = row.getCustomProperties();
                    if (foreignKey != 0) {
                        rpc.write(model, foreignKey, record);
                    } else {
                        final int primaryKey = rpc.create(model, record);
                        row.setId(String.valueOf(primaryKey));
                    }

                    int index = Collections.binarySearch(originalRowSet, row, Comparator.comparing(FormRow::getId));
                    if(index >= 0) {
                        // exclude from to be deleted row
                        originalRowSet.remove(index);
                    }

                    return row;
                }))

                .collect(Collectors.toCollection(() -> new FormRowSet(){{
                    setMultiRow(true);
                }}));

        // delete old data
        Optional.of(originalRowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(FormRow::getId)
                .filter(Objects::nonNull)
                .map(Try.onFunction(Integer::parseInt))
                .filter(Objects::nonNull)
                .forEach(Try.onConsumer(id -> rpc.unlink(model, id)));

        return storedRowSet;
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
                "/properties/form/OdooFormMultirowBinder.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Odoo"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }
}
