package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.*;
import java.util.stream.Collectors;

import com.kinnarastudio.kecakplugins.odoo.common.property.OdooAuthorizationUtil;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import com.kinnarastudio.kecakplugins.odoo.form.OdooOptionsBinder;

/**
 * @since 2025-11-03
 * @author aristo
 */
public class OdooOptionsValueFormatter extends DataListColumnFormatDefault {
    public final static String LABEL = "Odoo Options Value Formatter";

    Map<String, String> optionMap = null;

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        final String[] ids = getIds(dataList, column);
        final Map<String, String> options = getOptionMap(ids);

        if (value instanceof Object[]) {
            Object[] arrValue = (Object[]) value;

            LogUtil.debug(getClassName(), "Original Val: " + Arrays.toString(arrValue));

            if ("true".equals(getPropertyString("isMultiValue"))) {
                for (int i = 0; i < arrValue.length; i++) {
                    String current = String.valueOf(arrValue[i]);

                    if (options.containsKey(current)) {
                        String mapOptionString = options.get(current);

                        if (mapOptionString != null && !mapOptionString.equalsIgnoreCase("null")) {
                            arrValue[i] = options.get(current);
                        } else {
                            arrValue[i] = current;
                        }
                    } else {
                        arrValue[i] = current;
                    }
                }

                String[] stringArray = Arrays.stream(arrValue)
                        .map(obj -> obj == null ? "null" : obj.toString())
                        .toArray(String[]::new);

                String finalResult = String.join(";", stringArray);
                return finalResult;
            }

            return Optional.of(value)
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

        LogUtil.debug(getClassName(), "Val: [" + val + "]");

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

    protected Map<String, String> getOptionMap(String[] ids) {
        FormBinder optionBinder;
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        LogUtil.warn(getClassName(), "getOptionMap() called - instance: " + System.identityHashCode(this)
                + " optionMap: " + (optionMap == null ? "NULL" : "size=" + optionMap.size()));

        if (optionMap != null) {
            return optionMap;
        }

        optionMap = new HashMap<>();

        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");
        if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty() && (optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString())) != null) {
            optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));

            LogUtil.warn(getClassName(), "Calling load() - instance: " + System.identityHashCode(this));

            FormRowSet rowSet = ((FormAjaxOptionsBinder) optionBinder).loadAjaxOptions(ids);

            LogUtil.warn(getClassName(), "load() returned - rowSet: " + (rowSet == null ? "NULL" : "size=" + rowSet.size()));
            if (rowSet != null) {
                optionMap = new HashMap<>();
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
                LogUtil.warn(getClassName(), "optionMap filled - size: " + optionMap.size());
            }
        }
        return optionMap;
    }

    private String[] getIds(DataList dataList, DataListColumn column) {
        final String columnName = column.getName();

        // Step 1: Collect all unique IDs from the DataList rows
        Set<Integer> ids = Set.of();
        try {
            @SuppressWarnings("unchecked")
            Collection<Object> rows = dataList.getRows();
            ids = rows.stream()
                    .map(row -> {
                        Object val;
                        if (row instanceof Map) {
                            val = ((Map<?, ?>) row).get(columnName);
                        } else {
                            return null;
                        }
                        return extractId(val);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "Failed to collect IDs from DataList: " + e.getMessage());
        }

        if (ids.isEmpty()) {
            LogUtil.debug(getClassName(), "No IDs to look up for column [" + columnName + "]");
        }

        return ids.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    private Integer extractId(Object value) {
        if (value == null)
            return null;

        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= 1 && arr[0] instanceof Integer) {
                return (Integer) arr[0];
            }
            if (arr.length >= 1) {
                try {
                    return Integer.parseInt(String.valueOf(arr[0]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
