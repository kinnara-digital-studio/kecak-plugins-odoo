package com.kinnarastudio.kecakplugins.odoo.datalist;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.plugin.base.PluginManager;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OdooDataListFormatter extends DataListColumnFormatDefault {
    public final static String LABEL = "Odoo DataList Formatter";

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        if(value.getClass() == Object[].class) {
            return Arrays.stream((Object[])value)
                    .map(String::valueOf)
                    .collect(Collectors.joining(";"));
        }

        return String.valueOf(value);
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
        return "";
    }
}
