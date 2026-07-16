package com.kinnarastudio.kecakplugins.odoo.datalist.formatter;

import java.util.*;

import com.kinnarastudio.kecakplugins.odoo.common.property.CacheUtil;
import com.kinnarastudio.kecakplugins.odoo.form.OdooOptionsBinder;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
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

        Map<String, String> map = getOptionMap(dataList, column);

        if (value instanceof Object[]) {
            Object[] arrValue = (Object[]) value;

            LogUtil.info(getClassName(), "Original Val: " + Arrays.toString(arrValue));

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
        LogUtil.debug(getClassName(), "Val: [" + val + "]");
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

    protected Map<String, String> getOptionMap(DataList dataList, DataListColumn column) {
        FormBinder optionBinder;
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

        if (optionMap != null) {
            return optionMap;
        }

        optionMap = new HashMap<>();

        Map<String, Object> optionsBinderProperties = (Map<String, Object>) this.getProperty("optionsBinder");
        if (optionsBinderProperties != null && optionsBinderProperties.get("className") != null && !optionsBinderProperties.get("className").toString().isEmpty() && (optionBinder = (FormBinder) pluginManager.getPlugin(optionsBinderProperties.get("className").toString())) != null) {
            optionBinder.setProperties((Map) optionsBinderProperties.get("properties"));

            String[] ids = collectIdsFromDataList(dataList, column).toArray(new String[0]);

            LogUtil.warn(getClassName(), "Collecting ID and calling load() - instance: " + System.identityHashCode(this));

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

    @SuppressWarnings("unchecked")
    private Set<String> collectIdsFromDataList(DataList dataList, DataListColumn column) {
        final String columnName = column.getName();
        final boolean isMultiValue = "true".equals(getPropertyString("isMultiValue"));

        Collection<Object> rows;
        try {
            rows = (Collection<Object>) dataList.getRows();
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "Gagal ambil rows dari dataList untuk kolom [" + columnName + "]: " + e.getMessage());
            return Collections.emptySet();
        }

        if (rows == null) {
            return Collections.emptySet();
        }

        Set<String> ids = new HashSet<>();
        for (Object r : rows) {
            if (!(r instanceof Map)) continue;
            Object rawValue = ((Map<?, ?>) r).get(columnName);
            if (rawValue == null) continue;
            ids.addAll(extractIdsFromValue(rawValue, isMultiValue));
            //LogUtil.warn(getClassName(), "rows dari dataList untuk kolom [" + columnName + "]");
        }
        return ids;
    }

    private List<String> extractIdsFromValue(Object value, boolean isMultiValue) {
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;

            if (isMultiValue) {
                List<String> ids = new ArrayList<>();
                for (Object o : arr) {
                    String id = extractIdAsString(o);
                    if (id != null && !id.isEmpty()) ids.add(id);
                }
                return ids;
            }

            String id = extractIdAsString(value);
            return id != null && !id.isEmpty() ? Collections.singletonList(id) : Collections.emptyList();
        }

        String id = extractIdAsString(value);
        return id != null && !id.isEmpty() ? Collections.singletonList(id) : Collections.emptyList();
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
