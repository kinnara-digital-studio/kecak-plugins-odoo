package com.kinnarastudio.kecakplugins.odoo.app.webservice;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooAuthorizationException;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONException;
import org.json.JSONObject;
import org.kecak.apps.exception.ApiException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class OdooTestConnectionWebService extends DefaultApplicationPlugin implements PluginWebSupport {
    public final static String LABEL = "Odoo Test Connection Web Service";

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
    public Object execute(Map map) {
        return null;
    }

    @Override
    public void webService(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        String message;
        try {
            String baseUrl = getParameter(servletRequest, "baseUrl");
            String database = getParameter(servletRequest, "database");
            String user = getParameter(servletRequest, "user");
            String apiKey = getParameter(servletRequest, "apiKey");

            LogUtil.info(getClassName(), "webService baseUrl: " + baseUrl);
            LogUtil.info(getClassName(), "webService database: " + database);
            LogUtil.info(getClassName(), "webService user: " + user);
            LogUtil.info(getClassName(), "webService apiKey: " + apiKey);

            OdooRpc odooRpc = new OdooRpc(baseUrl, database, user, apiKey);

            boolean success =  odooRpc.login() > 0;
            message = success ? "Connection successful" : "Connection failed";

        } catch (ApiException | OdooAuthorizationException e) {
            message = e.getMessage();
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", message);
            servletResponse.getWriter().write(jsonObject.toString());
        } catch (JSONException e) {
            throw new ServletException(e);
        }
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
        return "";
    }

    protected String getParameter(@Nonnull HttpServletRequest request, @Nonnull String parameterName)
            throws ApiException {
        return optParameter(request, parameterName)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required parameter [" + parameterName + "]"));
    }

    protected Optional<String> optParameter(@Nonnull HttpServletRequest request, @Nonnull String parameterName) {
        Pattern p = Pattern.compile("#[^#]+#");

        return Optional.of(parameterName)
                .map(request::getParameter)
                .map(s -> {
                    int counter = 10;
                    while (p.matcher(s).find()) {
                        LogUtil.info(getClassName(), "Parameter [" + s + "] matches");
                        s = AppUtil.processHashVariable(s, null, null, null);
                        counter--;

                        if(counter <= 0) break;
                    }

                    return s;
                })
                .filter(Predicate.not(String::isEmpty));
    }
}
