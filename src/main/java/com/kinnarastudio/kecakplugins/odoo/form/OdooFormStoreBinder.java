package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
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

public class OdooFormStoreBinder extends FormBinder implements FormStoreElementBinder, FormDeleteBinder {
    public final static String LABEL = "Odoo Form Store Binder";

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final Map<String, Object> record = Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .map(Hashtable::entrySet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(e -> !e.getKey().toString().isEmpty() && Objects.nonNull(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

        try {
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
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] resources = new String[]{"/properties/common/OdooAuthorization.json"};

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(FormRow::getId)
                .map(Try.onFunction(Integer::parseInt))
                .forEach(Try.onConsumer(id -> rpc.unlink(model, id)));
    }
}
