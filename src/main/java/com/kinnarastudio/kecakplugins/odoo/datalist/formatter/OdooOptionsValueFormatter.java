package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadOptionsBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import com.kinnarastudio.kecakplugins.odoo.form.OdooOptionsBinder;

public class OdooOptionsValueFormatter extends DataListColumnFormatDefault {
    public final static String LABEL = "Odoo Options Value Formatter";

    Map<String, String> optionMap = null;


    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        final Map<String, String> options = getOptionMap();

        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            LogUtil.info(getClassName(), "Object Val: " + Arrays.toString(arr));
            return Optional.ofNullable(value)
                .filter(v -> v instanceof Object[])
                .map(v -> (Object[]) v)
                .stream()
                .flatMap(Arrays::stream)
                .findFirst()
                .map(val -> Optional.of(val)
                        .map(String::valueOf)
                        .map(s -> s.split(";"))
                        .stream()
                        .flatMap(Arrays::stream)
                        .filter(options::containsKey)
                        .map(options::get)
                        .collect(Collectors.joining(", ")))
                .orElseGet(() -> String.valueOf(value));
        }
        
        String val = String.valueOf(value);
        if (options.containsKey(val)) {
            return options.get(val);
        }

        return val;
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
        final Object[] args = new Object[] {
                FormLoadOptionsBinder.class.getName(),
                OdooOptionsBinder.class.getName()
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/formatter/OdooOptionsValueFormatter.json", args, true, null);
    }

    protected Map<String, String> getOptionMap() {
        FormBinder optionBinder;
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        if (this.optionMap != null) {
            return this.optionMap;
        }
        this.optionMap = new HashMap();

        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");
        if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty() && (optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString())) != null) {
            optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));
            FormRowSet rowSet = ((FormLoadBinder) optionBinder).load(null, null, null);
            if (rowSet != null) {
                this.optionMap = new HashMap();
                for (FormRow row : rowSet) {
                    String label;
                    Iterator it = row.stringPropertyNames().iterator();
                    String value = row.getProperty("value");
                    if (value == null) {
                        Object key = it.next();
                        value = row.getProperty(String.valueOf(key));
                    }
                    if ((label = row.getProperty("label")) == null) {
                        Object key = it.next();
                        label = row.getProperty(String.valueOf(key));
                    }
                    this.optionMap.put(value, label);
                }
            }
        }
        return this.optionMap;
    }
}
