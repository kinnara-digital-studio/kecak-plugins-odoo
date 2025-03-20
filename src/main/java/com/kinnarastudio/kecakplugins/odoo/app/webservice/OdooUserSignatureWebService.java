package com.kinnarastudio.kecakplugins.odoo.app.webservice;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.plugin.property.service.PropertyUtil;
import org.kecak.apps.exception.ApiException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Load signature image form odoo's server model <i>res.users</i> field <i>sign_signature</i>
 */
public class OdooUserSignatureWebService extends DefaultApplicationPlugin implements PluginWebSupport {
    public final static int BUFFER_LENGTH = 1024;
    public final static String LABEL = "Odoo User Signature Web Service";

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
        try {
            final String method = servletRequest.getMethod();
            if (!"GET".equalsIgnoreCase(method)) {
                throw new ApiException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method [" + method + "] is not supported");
            }

            final int userId = optParameter(servletRequest, "userId")
                    .map(Try.onFunction(Integer::parseInt))
                    .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Parameter userId is not supplied"));

            final String baseUrl = getBaseUrl();
            final String database = getDatabase();
            final String username = getUsername();
            final String apiKey = getApiKey();

            final OdooRpc odooRpc = new OdooRpc(baseUrl, database, username, apiKey);
            final Map<String, Object> userRecord = odooRpc.read("res.users", userId)
                    .orElseThrow(() -> new ApiException(HttpServletResponse.SC_NOT_FOUND, ""));

            final String base64Signature = String.valueOf(userRecord.get("sign_signature"));

            final boolean asBase64 = optParameter(servletRequest, "format")
                    .map("base64"::equalsIgnoreCase)
                    .orElse(false);

            if(asBase64) {
                servletResponse.getWriter().write(base64Signature);
            } else {
                servletResponse.setContentType("image/png");
                final byte[] decodedSignature = Base64.getDecoder().decode(base64Signature.getBytes(StandardCharsets.UTF_8));
                try (InputStream inputStream = new ByteArrayInputStream(decodedSignature)) {
                    final OutputStream outputStream = servletResponse.getOutputStream();
                    byte[] buffer = new byte[BUFFER_LENGTH];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            }

        } catch (ApiException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            servletResponse.sendError(e.getErrorCode(), e.getMessage());
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
        return AppUtil.readPluginResource(getClassName(), "/properties/common/OdooAuthorization.json");
    }

    protected Optional<String> optParameter(HttpServletRequest servletRequest, String name) {
        return Optional.of(name)
                .map(servletRequest::getParameter);
    }

    protected String getParameter(HttpServletRequest servletRequest, String name) throws ApiException {
        return optParameter(servletRequest, name)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Parameter [" + name + "] is not supplied"));
    }

    protected String getBaseUrl() {
        return getPropertyString("baseUrl");
    }

    protected String getUsername() {
        return getPropertyString("user");
    }

    protected String getDatabase() {
        return getPropertyString("database");
    }

    protected String getApiKey() {
        return getPropertyString("apiKey");
    }

    @Override
    public Map<String, Object> getProperties() {
        PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        return Optional.ofNullable(pluginDefaultPropertiesDao.loadById(getClassName(), appDefinition))
                .map(PluginDefaultProperties::getPluginProperties)
                .map(s -> AppUtil.processHashVariable(s, null, StringUtil.TYPE_JSON, null))
                .map(PropertyUtil::getPropertiesValueFromJson)
                .orElseGet(Collections::emptyMap);
    }

    @Override
    public String getPropertyString(String property) {
        Map<String, Object> properties = getProperties();
        String value = properties != null && properties.get(property) != null ? (String) properties.get(property) : "";
        return value;
    }
}