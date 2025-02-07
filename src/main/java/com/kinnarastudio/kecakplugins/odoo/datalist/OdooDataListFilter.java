package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.IOdooFilter;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooFilterQueryObject;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterType;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.plugin.base.PluginManager;

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
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());

        if(getValue(dataList, name) == null) {
            return null;
        }

        final String value;
        final String operator;

        final String mode = getMode();
        if("startsWith".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name) + "%";
            operator = "=ilike";
        } else if("contains".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name);
            operator = "ilike";
        } else if("custom".equalsIgnoreCase(mode)) {
            value = getValue(dataList, name);
            name = getConditionField(name);
            operator = getConditionOperator();
        } else {
            value = getValue(dataList, name);
            operator = "=";
        }

        final IOdooFilter.DataType dataType = getDataType();
        final DataListFilterQueryObject filterQueryObject = new OdooFilterQueryObject(name, operator, value, dataType);
        return filterQueryObject;
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/OdooDataListFilter.json");
    }

    protected Map<String, Object> getFilterPlugin() {
        return (Map<String, Object>) getProperty("filterPlugin");
    }

    protected String getMode() {
        return getPropertyString("mode");
    }

    protected IOdooFilter.DataType getDataType () {
        return "integer".equalsIgnoreCase(getPropertyString("dataType")) ? IOdooFilter.DataType.INTEGER : IOdooFilter.DataType.STRING;
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

    protected String getConditionOperator() {
        return getPropertyString("conditionOperator");
    }
}
