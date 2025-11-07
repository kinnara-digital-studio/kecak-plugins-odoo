package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooFormMultirowLoadBinderUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.DataType;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.Field;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.apache.commons.lang3.tuple.Pair;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.app.service.AuditTrailManager;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Odoo Form MultiRow Binder
 */
public class OdooFormMultirowBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormLoadMultiRowElementBinder, FormStoreMultiRowElementBinder {
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
                        m.forEach((key, value) -> {
                            if (value instanceof Object[] && ((Object[]) value).length > 0) {
                                String strValue = Optional.of((Object[]) value)
                                        .stream()
                                        .flatMap(Arrays::stream)
                                        .findFirst()
                                        .map(String::valueOf)
                                        .orElse("");

                                setProperty(key, strValue);
                            } else {
                                setProperty(key, String.valueOf(value));
                            }
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

        if (rowSet == null) return null;

        try {
            AuditTrailManager auditTrailManager = (AuditTrailManager) AppUtil.getApplicationContext().getBean("auditTrailManager");

            final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
            final String database = OdooAuthorizationUtil.getDatabase(this);
            final String user = OdooAuthorizationUtil.getUsername(this);
            final String apiKey = OdooAuthorizationUtil.getApiKey(this);
            final String model = OdooAuthorizationUtil.getModel(this);
            final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey, auditTrailManager);

            final Collection<Field> fields = rpc.fieldsGet(model);

            final String foreignKeyField = OdooFormMultirowLoadBinderUtil.getForeignKeyField(this);

            final Form parentForm = FormUtil.findRootForm(element);
            final int parentRecordId = Optional.ofNullable(parentForm.getPrimaryKeyValue(formData))
                    .map(Try.onFunction(Integer::parseInt, (NumberFormatException e) -> 0))
                    .orElse(0);

            final FormRowSet originalRowSet = Optional.ofNullable(load(element, String.valueOf(parentRecordId), formData))
                    .orElseGet(FormRowSet::new);
            originalRowSet.sort(Comparator.comparing(FormRow::getId));

            final FormRowSet storedRowSet = rowSet.stream()
                    .map(Try.onFunction(row -> {
                        row.setProperty(foreignKeyField, String.valueOf(parentRecordId));

                        final int foreignKey = Optional.ofNullable(row.getId())
                                .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> null))
                                .orElse(0);

                        final Map<String, Object> record = fields.stream()
                                .map(Try.onFunction(f -> {
                                    final DataType type = f.getType();
                                    final String strValue = row.getProperty(f.getKey());

                                    if(strValue == null) return null;

                                    final Object value;
                                    if (type == DataType.INTEGER) {
                                        value = Integer.parseInt(strValue);
                                    } else {
                                        value = strValue;
                                    }

                                    return Pair.of(f.getKey(), value);
                                }, (Exception e) -> null))
                                .filter(p -> Objects.nonNull(p) && Objects.nonNull(p.getRight()))
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
                        if (foreignKey != 0) {
                            rpc.write(model, foreignKey, record);
                        } else {
                            final int primaryKey = rpc.create(model, record);
                            row.setId(String.valueOf(primaryKey));
                        }

                        int index = Collections.binarySearch(originalRowSet, row, Comparator.comparing(FormRow::getId));
                        if (index >= 0) {
                            // exclude from to be deleted row
                            originalRowSet.remove(index);
                        }

                        return row;
                    }))

                    .collect(Collectors.toCollection(() -> new FormRowSet() {{
                        setMultiRow(true);
                    }}));

            // delete old data
            final int[] ids = Optional.of(originalRowSet).stream()
                    .flatMap(Collection::stream)
                    .map(FormRow::getId)
                    .filter(Objects::nonNull)
                    .map(Try.onFunction(Integer::parseInt))
                    .filter(Objects::nonNull)
                    .mapToInt(i -> i)
                    .toArray();

            Arrays.stream(ids)
                    .boxed()
                    .forEach(i -> {
                        try {
                            rpc.unlink(model, i);
                        } catch (OdooCallMethodException e) {
                            LogUtil.error(getClassName(), e, "Error unlinking model [" + model + "] record [" + i + "]");
                        }
                    });

            return storedRowSet;
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
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
