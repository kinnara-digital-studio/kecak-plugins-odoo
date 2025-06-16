package com.kinnarastudio.kecakplugins.odoo.app;

import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.service.PropertyUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Odoo Search Read Hash Variable
 *
 * Syntax : odooSearchRead.READ_FIELD.MODEL[FIELD OPERATOR VALUE][FIELD OPERATOR VALUE]...
 */
public class OdooSearchReadHashVariable extends DefaultHashVariablePlugin {
    public final static String LABEL = "Odoo Search Read Hash Variable";

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
        return AppUtil.readPluginResource(getClassName(), "/properties/common/OdooAuthorization.json");
    }

    @Override
    public String getPrefix() {
        return "odooSearchRead";
    }

    @Override
    public String processHashVariable(String key) {
        final String baseUrl = OdooAuthorizationUtil.getBaseUrl(this);
        final String database = OdooAuthorizationUtil.getDatabase(this);
        final String user = OdooAuthorizationUtil.getUsername(this);
        final String apiKey = OdooAuthorizationUtil.getApiKey(this);
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        final String[] split = key.replaceAll("\\[.+]", "").split("\\.", 2);
        final String field = Arrays.stream(split).findFirst().orElse("");
        final String model = Arrays.stream(split).skip(1).findFirst().orElse("");
        final Matcher matcher = Pattern.compile("(?<=\\[)(\\w+)\\s([^\\s]+)\\s(.+)(?=])").matcher(key);
        final List<SearchFilter> filters = new ArrayList<>();
        while (matcher.find()) {
            final String filterField = matcher.group(1);
            final String operator = matcher.group(2);
            final String value = matcher.group(3).replaceAll("^'|'$", "");
            filters.add(new SearchFilter(filterField, operator, value));
        }

        try {
            return Optional.of(rpc.searchRead(model, filters.toArray(new SearchFilter[0]), null, null, 1))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(m -> m.get(field))
                    .map(o -> {
                        if(o instanceof Object[]) {
                            return ((Object[])o)[0];
                        } else {
                            return o;
                        }
                    })
                    .map(String::valueOf)
                    .collect(Collectors.joining(";"));
        } catch (OdooCallMethodException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return "";
        }
    }

    @Override
    public Collection<String> availableSyntax() {
        return new ArrayList<>() {{
            add(getPrefix() + ".READ_FIELD.MODEL[FIELD OPERATOR VALUE]");
            add(getPrefix() + ".READ_FIELD.MODEL[FIELD OPERATOR VALUE][FIELD OPERATOR VALUE]...");
        }};
    }

    public Map<String, Object> getDefaultProperties() {
        PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        return Optional.ofNullable(pluginDefaultPropertiesDao.loadById(getClassName(), appDefinition))
                .map(PluginDefaultProperties::getPluginProperties)
                .map(s -> AppUtil.processHashVariable(s, null, StringUtil.TYPE_JSON, null))
                .map(PropertyUtil::getPropertiesValueFromJson)
                .orElseGet(Collections::emptyMap);
    }
}
