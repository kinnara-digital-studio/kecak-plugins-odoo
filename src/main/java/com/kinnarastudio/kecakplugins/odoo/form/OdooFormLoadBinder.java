package com.kinnarastudio.kecakplugins.odoo.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.OdooUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.ResourceBundle;

public class OdooFormLoadBinder extends FormBinder implements FormLoadElementBinder {
    public final static String LABEL = "Odoo Form Load Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        if(primaryKey == null) return null;

        final String baseUrl = OdooUtil.getBaseUrl(this);
        final String database = OdooUtil.getDatabase(this);
        final String user = OdooUtil.getUsername(this);
        final String apiKey = OdooUtil.getApiKey(this);
        final String model = OdooUtil.getModel(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        try {
            return rpc.read(model, Integer.parseInt(primaryKey))
                    .map(m -> new FormRow() {{
                        m.forEach((k, v) -> {
                            if (v != null) setProperty(k, String.valueOf(v));
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
}
