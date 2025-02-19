package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.plugin.base.PluginManager;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OdooObjectDataListFormatter extends DataListColumnFormatDefault {
    public final static String LABEL = "Odoo Object DataList Formatter";

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        if (value instanceof Object[]) {
            final Object[] values = (Object[]) value;
            if (isAsOptions()) {
                return Arrays.stream(values).skip(1)
                        .findFirst()
                        .map(String::valueOf)
                        .orElse("");
            } else {
                return Arrays.stream(values)
                        .map(String::valueOf)
                        .collect(Collectors.joining(";"));
            }
        } else if (asDecodedImage()) {
            return String.format("<img src='data:image/png;base64, %s' />", value);
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
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/formatter/OdooObjectDataListFormatter.json");
    }

    protected String getFormattingType() {
        return getPropertyString("formattingType");
    }

    protected boolean isAsOptions() {
        return "asOptions".equalsIgnoreCase(getFormattingType());
    }

    protected boolean asDecodedImage() {
        return "asDecodedImage".equalsIgnoreCase(getFormattingType());
    }

    protected boolean isJoiningArrayOfObject() {
        return "joiningArrayOfObject".equalsIgnoreCase(getFormattingType());
    }
}
