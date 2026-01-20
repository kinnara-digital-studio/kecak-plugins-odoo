package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import com.kinnarastudio.odooxmlrpc.rpc.OdooRpc;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Odoo Custom Key Form Binder
 *
 * Load odoo data using field other than ID
 */
public class OdooCustomKeyFormBinder extends FormBinder implements FormLoadBinder {
    public final static String LABEL = "Odoo Custom Key Form Binder";

    @Override
    public FormRowSet load(Element element, String customKey, FormData formData) {
        if (customKey == null) return null;

        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final String model = OdooAuthorizationUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        try {
            String keyField = getKeyField();

            return Optional.ofNullable(rpc.searchRead(model, SearchFilter.single(keyField, customKey), null, null, 1))
                    .stream()
                    .flatMap(Arrays::stream)
                    .findFirst()
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
        return OdooCustomKeyFormBinder.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] resources = new String[]{
                "/properties/common/OdooAuthorization.json",
                "/properties/form/OdooCustomKeyFormBinder.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }

    protected String getKeyField() {
        return getPropertyString("keyField");
    }
}
