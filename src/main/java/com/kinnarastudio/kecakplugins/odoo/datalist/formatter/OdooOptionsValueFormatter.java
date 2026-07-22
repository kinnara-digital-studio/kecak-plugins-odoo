package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.*;

import com.kinnarastudio.kecakplugins.odoo.form.OdooOptionsBinder;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.model.*;
import org.joget.plugin.base.PluginManager;

/**
 * @since 2025-11-03
 * @author aristo
 */
public class OdooOptionsValueFormatter extends DataListColumnFormatDefault {
    public final static String LABEL = "Odoo Options Value Formatter";

    Map<String, String> optionMap = null;

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {

        Map<String, String> map = getOptionMap();

        if (value instanceof Object[]) {
            Object[] arrValue = (Object[]) value;

            if ("true".equals(getPropertyString("isMultiValue"))) {
                for (int i = 0; i < arrValue.length; i++) {
                    String current = extractIdAsString(arrValue[i]);
                    String mapped = map.get(current);
                    arrValue[i] = (mapped != null && !mapped.equalsIgnoreCase("null")) ? mapped : current;
                }

                String[] stringArray = Arrays.stream(arrValue)
                        .map(obj -> obj == null ? "null" : obj.toString())
                        .toArray(String[]::new);

                return String.join(";", stringArray);
            }

            String id = extractIdAsString(value);
            String label = map.get(id);
            return label != null ? label : id;
        }

        String val = extractIdAsString(value);
        String label = map.get(val);
        return label != null ? label : val;
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

        if (optionMap != null) {
            return optionMap;
        }

        optionMap = new HashMap<>();

        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");
        if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty() && (optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString())) != null) {
            optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));

            FormRowSet rowSet = ((FormAjaxOptionsBinder) optionBinder).loadAjaxOptions(null);

            if (rowSet != null) {
                for (FormRow row : rowSet) {
                    String label;
                    Iterator<String> i = row.stringPropertyNames().iterator();
                    String value = row.getProperty("value");
                    if (value == null) {
                        String key = i.next();
                        value = row.getProperty(key);
                    }
                    if ((label = row.getProperty("label")) == null) {
                        String key = i.next();
                        label = row.getProperty(key);
                    }
                    optionMap.put(value, label);
                }
            }
        }
        return optionMap;
    }

    private String extractIdAsString(Object value) {
        if (value == null) return null;
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            return arr.length > 0 ? String.valueOf(arr[0]) : null;
        }
        return String.valueOf(value);
    }
}
