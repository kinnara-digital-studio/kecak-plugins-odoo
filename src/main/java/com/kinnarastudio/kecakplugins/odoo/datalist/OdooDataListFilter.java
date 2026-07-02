package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.DataType;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooFilterQueryObject;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterType;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OdooDataListFilter extends DataListFilterTypeDefault {
    public final static String LABEL = "Odoo DataList Filter";

    final Pattern p = Pattern.compile("(\\$)");

    @Override
    public String getTemplate(DataList dataList, String name, String label) {
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());
        return filterPlugin.getTemplate(dataList, name, label);
    }

    @Override
    public DataListFilterQueryObject getQueryObject(DataList dataList, String name) {
        if (getValue(dataList, name) == null) {
            return null;
        }

        final String value;
        final SearchFilter.Operator operator;

        final String mode = getMode();
        if ("startsWith".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name) + "%";
            operator = SearchFilter.Operator.ILIKE;
        } else if ("contains".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name);
            operator = SearchFilter.Operator.ILIKE;
        } else if ("custom".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name);
            name = getConditionField(name);
            operator = getConditionOperator();
        } else {
            value = getValue(dataList, name);
            operator = SearchFilter.Operator.EQUAL;
        }

        final com.kinnarastudio.odooxmlrpc.model.DataType dataType = getDataType();

        return new OdooFilterQueryObject(name, operator, value, dataType);
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
        final Object[] args = new Object[]{
                new JSONArray() {{
                    for (DataType value : DataType.values()) {
                        put(new JSONObject() {{
                            try {
                                put("value", value.name());
                                put("label", value.name());
                            } catch (JSONException ignored) {
                            }
                        }});
                    }
                }}.toString()
        };

        return AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/OdooDataListFilter.json", args, true);
    }

    protected Map<String, Object> getFilterPlugin() {
        return (Map<String, Object>) getProperty("filterPlugin");
    }

    protected String getMode() {
        return getPropertyString("mode");
    }

    protected com.kinnarastudio.odooxmlrpc.model.DataType getDataType() {
        return com.kinnarastudio.odooxmlrpc.model.DataType.parse(getPropertyString("dataType"));
    }

    protected String getConditionField(String name) {
        final Matcher m = p.matcher(getPropertyString("conditionField"));
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, name);
        }

        m.appendTail(sb);
        return sb.toString();
    }

    protected SearchFilter.Operator getConditionOperator() {
        return SearchFilter.Operator.parse(getPropertyString("conditionOperator"));
    }
}
