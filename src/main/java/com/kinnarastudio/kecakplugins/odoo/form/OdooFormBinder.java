package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.Field;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.app.service.AuditTrailManager;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Load Odoo data based on Odoo's primary key
 */
public class OdooFormBinder extends FormBinder implements FormLoadBinder, FormStoreBinder, FormDeleteBinder {
    public final static String LABEL = "Odoo Form Binder";

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
            return rpc.read(model, Integer.parseInt(primaryKey))
                    .map(m -> new FormRow() {{
                        m.forEach((k, v) -> {
                            if (v != null) {
                                final String value;
                                if (v instanceof Object[]) {
                                    value = Arrays.stream((Object[]) v)
                                            .map(String::valueOf)
                                            .collect(Collectors.joining(";"));
                                } else {
                                    value = String.valueOf(v);
                                }

                                setProperty(k, value);
                            }
                        });
                    }})
                    .map(r -> new FormRowSet() {{
                        add(r);
                    }})

                    .orElse(null);
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        AuditTrailManager auditTrailManager = (AuditTrailManager) AppUtil.getApplicationContext().getBean("auditTrailManager");

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey, auditTrailManager);

        try {
            final Collection<Field> fields = rpc.fieldsGet(model);

            final Map<String, Object> record = Optional.ofNullable(rowSet)
                    .stream()
                    .flatMap(Collection::stream)
                    .findFirst()
                    .map(FormRow::entrySet)
                    .stream()
                    .flatMap(Collection::stream)
                    .peek(Try.onConsumer(e -> {
                        final String childId = String.valueOf(e.getKey());

                        final Element child = FormUtil.findElement(childId, element, formData);
                        if (child == null) {
                            return;
                        }

                        final FormLoadBinder optionsBinder = child.getOptionsBinder();
                        if (optionsBinder instanceof OdooOptionsBinder) {
                            e.setValue(Integer.parseInt(String.valueOf(e.getValue())));
                        }
                    }))
                    .filter(e -> fields.contains(String.valueOf(e.getKey())) && Objects.nonNull(e.getValue()))
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));

            final int recordId = Optional.ofNullable(record.get("id"))
                    .map(String::valueOf)
                    .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> null))
                    .orElseGet(() -> Optional.ofNullable(formData.getPrimaryKeyValue())
                            .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> null))
                            .orElse(0));

            if (recordId != 0) {
                rpc.write(model, recordId, record);
            } else {
                final int primaryKey = rpc.create(model, record);
                if (rowSet != null) rowSet.forEach(row -> row.setId(String.valueOf(primaryKey)));
            }

            return rowSet;
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            final Form rootForm = FormUtil.findRootForm(element);
            final String id = rootForm.getPropertyString(FormUtil.PROPERTY_ID);
            formData.addFormError(id, e.getMessage());

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
        return OdooFormBinder.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] resources = new String[]{
                "/properties/common/OdooAuthorization.json",
                "/properties/form/OdooFormBinder.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        AuditTrailManager auditTrailManager = (AuditTrailManager) AppUtil.getApplicationContext().getBean("auditTrailManager");

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey, auditTrailManager);

        final int[] ids = Optional.ofNullable(rowSet).stream()
                .flatMap(Collection::stream)
                .map(FormRow::getId)
                .map(Try.onFunction(Integer::parseInt))
                .mapToInt(i -> i)
                .toArray();


        IntStream.of(ids).forEach(i -> {
            try {
                rpc.unlink(model, i);
            } catch (OdooCallMethodException e) {
                LogUtil.error(getClassName(), e, "Error unlinking model [" + model + "] record [" + i + "]");
            }
        });
    }
}
