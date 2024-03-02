package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooFilterQueryObject;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterType;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.plugin.base.PluginManager;

import java.util.Map;
import java.util.ResourceBundle;

public class OdooDataListFilter extends DataListFilterTypeDefault {
    public final static String LABEL = "Odoo DataList Filter";

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
            value = "%" + getValue(dataList, name) + "%";
            operator = "=ilike";
        } else {
            value = getValue(dataList, name);
            operator = "=";
        }

        final DataListFilterQueryObject filterQueryObject = new OdooFilterQueryObject(name, operator, value);
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
}
