package com.kinnarastudio.kecakplugins.odoo.app;

import com.kinnarastudio.kecakplugins.odoo.common.property.CacheUtil;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.apache.commons.lang3.tuple.Triple;
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

        final String cacheKey = CacheUtil.getCacheKey(this.getClass(), database, user, key);
        final String cached = (String) CacheUtil.getCached(cacheKey);
        if(cached != null && !cached.isEmpty()) {
            LogUtil.debug(getClassName(), "Cache hit for key " + cacheKey);
            LogUtil.info(getClassName(), "Cache hit for key " + cacheKey);
            return cached;
        }

        final String[] split = key.replaceAll("\\[.+]|\\(.+\\)", "").split("\\.", 2);
        final String field = Arrays.stream(split).findFirst().orElse("");
        final String model = Arrays.stream(split).skip(1).findFirst().orElse("");

        // [filter, offset, limit]
        final Triple<List<SearchFilter>, Integer, Integer> filters;

        if (key.contains("(")) {
            filters = getReadParameters(key);
        } else {
            // Legacy support for filters using []
            filters = getLegacyReadParameters(key);
        }

        try {
            final String ret = Optional.of(rpc.searchRead(model, filters.getLeft().toArray(new SearchFilter[0]), null, filters.getMiddle(), filters.getRight()))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(m -> m.get(field))
                    .map(o -> {
                        if (o instanceof Object[]) {
                            return ((Object[]) o)[0];
                        } else {
                            return o;
                        }
                    })
                    .map(String::valueOf)
                    .collect(Collectors.joining(";"));

            return CacheUtil.putCache(cacheKey, ret);

        } catch (OdooCallMethodException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return "";
        }
    }

    @Override
    public Collection<String> availableSyntax() {
        return new ArrayList<>() {{
            add(getPrefix() + ".READ_FIELD.MODEL[FIELD OPERATOR VALUE][FIELD OPERATOR VALUE]...");
            add(getPrefix() + ".READ_FIELD.MODEL(FIELD OPERATOR VALUE)(FIELD OPERATOR VALUE)...");
            add(getPrefix() + ".READ_FIELD.MODEL(FIELD OPERATOR VALUE)(FIELD OPERATOR VALUE)...[INDEX]");
            add(getPrefix() + ".READ_FIELD.MODEL(FIELD OPERATOR VALUE)(FIELD OPERATOR VALUE)...[INDEX_FROM-INDEX_TO]");
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

    private final static Pattern filterPattern = Pattern.compile("(?<=\\()(\\w+)\\s(\\S+)\\s(.+?)(?=\\))");
    private final static Pattern indexPattern = Pattern.compile("(?<=\\[)(\\d+)(-(\\d+))?(?=])");

    /**
     *
     * @param key
     * @return [filder, offset, limit]
     */
    protected Triple<List<SearchFilter>, Integer, Integer> getReadParameters(String key) {
        final Matcher indexMatcher = indexPattern.matcher(key);
        final String strIndex = indexMatcher.find() ? indexMatcher.group() : "";
        final int offset = getOffset(strIndex);
        final int limit = getLimit(strIndex);

        final Matcher filterMatcher = filterPattern.matcher(key);
        final List<SearchFilter> filters = new ArrayList<>();
        while (filterMatcher.find()) {
            final String filterField = filterMatcher.group(1);
            final String operator = filterMatcher.group(2);
            final String value = filterMatcher.group(3);

            if(value.matches("\\d+")) {
                // numeric filter, most likely IDs
                filters.add(new SearchFilter(filterField, operator, Integer.parseInt(value)));
            } else {
                // string filter
                filters.add(new SearchFilter(filterField, operator, value.replaceAll("^'|'$", "")));
            }
        }
        return Triple.of(filters, offset, limit);
    }

    protected Triple<List<SearchFilter>, Integer, Integer> getLegacyReadParameters(String key) {
        final Matcher filterMatcher = Pattern.compile("(?<=\\[)(\\w+)\\s(\\S+)\\s(.+)(?=])").matcher(key);
        final List<SearchFilter> filters = new ArrayList<>();
        while (filterMatcher.find()) {
            final String filterField = filterMatcher.group(1);
            final String operator = filterMatcher.group(2);
            final String value = filterMatcher.group(3);

            if(value.matches("\\d+")) {
                // numeric filter, most likely IDs
                filters.add(new SearchFilter(filterField, operator, Integer.parseInt(value)));
            } else {
                // string filter
                filters.add(new SearchFilter(filterField, operator, value.replaceAll("^'|'$", "")));
            }
        }
        return Triple.of(filters, 0, 1);
    }

    protected int getOffset(String input) {
        assert indexPattern.matcher(input).find();

        if(input.contains("-")) {
            final String[] split = input.split("-");
            return Arrays.stream(split).findFirst()
                    .map(Integer::parseInt)
                    .orElse(0);
        } else {
            return Integer.parseInt(input);
        }
    }

    protected int getLimit(String input) {
        assert indexPattern.matcher(input).find();

        if(input.contains("-")) {
            final String[] split = input.split("-");
            return Arrays.stream(split)
                    .skip(1)
                    .findFirst()
                    .map(Integer::parseInt)
                    .orElse(0);
        } else {
            return 0;
        }
    }
}
