package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.*;
import java.util.stream.Collectors;

import com.kinnarastudio.kecakplugins.odoo.common.property.CacheUtil;
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

    //Map<String, String> optionMap = null;

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {

        if (value instanceof Object[]) {
            Object[] arrValue = (Object[]) value;

            LogUtil.debug(getClassName(), "Original Val: " + Arrays.toString(arrValue));

            if ("true".equals(getPropertyString("isMultiValue"))) {
                for (int i = 0; i < arrValue.length; i++) {
                    String current = extractIdAsString(arrValue[i]); // <-- ganti String.valueOf jadi extractIdAsString
                    String mapped = getLabelById(current);
                    arrValue[i] = (mapped != null && !mapped.equalsIgnoreCase("null")) ? mapped : current;
                }

                String[] stringArray = Arrays.stream(arrValue)
                        .map(obj -> obj == null ? "null" : obj.toString())
                        .toArray(String[]::new);

                String finalResult = String.join(";", stringArray);
                return finalResult;
            }

            String id = extractIdAsString(value); // ambil ID dari [42, "John"]
            String label = getLabelById(id);
            return label != null ? label : id;
        }

        String val = extractIdAsString(value);
        LogUtil.debug(getClassName(), "Val: [" + val + "]");
        String label = getLabelById(val);
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

    /**
     * Lookup label untuk satu ID.
     * Cache per ID di CacheUtil — key: className + columnName + id
     * Kalau belum ada di cache, fetch ke Odoo hanya untuk ID ini.
     */
    private String getLabelById(String id) {
        if (id == null || id.isEmpty()) return null;

        String cacheKey = getClassName() + "_label_" + id;
        String cached = (String) CacheUtil.getCached(cacheKey);
        if (cached != null) {
            LogUtil.debug(getClassName(), "Cache hit ID: " + id + " → " + cached);
            return cached;
        }

        // Fetch ke Odoo hanya untuk ID ini
        Map<String, String> map = fetchOptionMap(new String[]{id});
        String label = map.get(id);

        if (label != null) {
            CacheUtil.putCache(cacheKey, label);
        }

        return label;
    }

//    protected Map<String, String> getOptionMap(String[] ids) {
//        FormBinder optionBinder;
//        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
//        LogUtil.warn(getClassName(), "getOptionMap() called - instance: " + System.identityHashCode(this)
//                + " optionMap: " + (optionMap == null ? "NULL" : "size=" + optionMap.size()));
//
//        if (optionMap != null) {
//            return optionMap;
//        }
//
//        optionMap = new HashMap<>();
//
//        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");
//        if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty() && (optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString())) != null) {
//            optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));
//
//            LogUtil.warn(getClassName(), "Calling load() - instance: " + System.identityHashCode(this));
//
//            FormRowSet rowSet = ((FormAjaxOptionsBinder) optionBinder).loadAjaxOptions(ids);
//
//            LogUtil.warn(getClassName(), "load() returned - rowSet: " + (rowSet == null ? "NULL" : "size=" + rowSet.size()));
//            if (rowSet != null) {
//                optionMap = new HashMap<>();
//                for (FormRow row : rowSet) {
//                    String label;
//                    Iterator<String> i = row.stringPropertyNames().iterator();
//                    String value = row.getProperty("value");
//                    if (value == null) {
//                        String key = i.next();
//                        value = row.getProperty(key);
//                    }
//                    if ((label = row.getProperty("label")) == null) {
//                        String key = i.next();
//                        label = row.getProperty(key);
//                    }
//                    optionMap.put(value, label);
//                }
//                LogUtil.warn(getClassName(), "optionMap filled - size: " + optionMap.size());
//            }
//        }
//        return optionMap;
//    }

    private Map<String, String> fetchOptionMap(String[] ids) {
        Map<String, String> result = new HashMap<>();

        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");

        if (optionsBinderProperties == null
                || optionsBinderProperties.get("className") == null
                || optionsBinderProperties.get("className").toString().isEmpty()) {
            return result;
        }

        FormBinder optionBinder = (FormBinder) pluginManager.getPlugin(
                optionsBinderProperties.get("className").toString());
        if (optionBinder == null || !(optionBinder instanceof FormAjaxOptionsBinder)) {
            return result;
        }

        optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));

        try {
            FormRowSet rowSet = ((FormAjaxOptionsBinder) optionBinder).loadAjaxOptions(ids);
            if (rowSet != null) {
                for (FormRow r : rowSet) {
                    String value = r.getProperty("value");
                    String label = r.getProperty("label");
                    if (value != null && !value.isEmpty()) {
                        result.put(normalizeKey(value), label != null ? label : value);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "fetchOptionMap gagal untuk IDs "
                    + Arrays.toString(ids) + ": " + e.getMessage());
        }

        return result;
    }

    private String extractIdAsString(Object value) {
        if (value == null) return null;
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            return arr.length > 0 ? normalizeKey(arr[0]) : null;
        }
        return normalizeKey(value);
    }

    private String normalizeKey(Object val) {
        if (val == null) return "";
        if (val instanceof Double) {
            double d = (Double) val;
            if (d == Math.floor(d)) return String.valueOf((long) d);
        }
        return String.valueOf(val);
    }
}
