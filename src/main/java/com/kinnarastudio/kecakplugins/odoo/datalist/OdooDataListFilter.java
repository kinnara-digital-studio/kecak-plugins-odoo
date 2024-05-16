package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooFilterQueryObject;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterType;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.apps.form.model.FormAjaxOptionsBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.util.WorkflowUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

public class OdooDataListFilter extends DataListFilterTypeDefault{
    public final static String LABEL = "Odoo DataList Filter";

    private final static DateFormat hibernateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getTemplate(DataList dataList, String name, String label) {
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        // final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());
        // return filterPlugin.getTemplate(dataList, name, label);
        
        Map dataModel = new HashMap();
        
        dataModel.put("name", dataList.getDataListEncodedParamName(DataList.PARAMETER_FILTER_PREFIX+name));
        dataModel.put("label", label);
        dataModel.put("value", getValue(dataList, name, getPropertyString("defaultValue")));

        //Text Field
        dataModel.put("contextPath", WorkflowUtil.getHttpServletRequest().getContextPath());

        //Date Time
        boolean showTime = "true".equals(getPropertyString("showTime"));
        dataModel.put("dateFormat", showTime ? "yyyy-mm-dd hh:ii:ss" : "yyyy-mm-dd");

        final String[] defaultValue = AppUtil.processHashVariable(getPropertyString("defaultValue"), null, null, null).split(";", 2);
        final String defaultValueFrom = Arrays.stream(defaultValue).findFirst().orElse("");
        final String defaultValueTo = Arrays.stream(defaultValue).skip(1).findFirst().orElse("");

        dataModel.put("valueFrom", getValue(dataList, name + "-from", defaultValueFrom));
        dataModel.put("valueTo", getValue(dataList, name + "-to", defaultValueTo));
        dataModel.put("optionsBinder", getProperty("optionsBinder"));
        dataModel.put("className", getClassName());
        dataModel.put("minView", showTime ? "hour" : "month");
        dataModel.put("singleValue", getPropertyString("singleValue"));

        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        dataModel.put("request", request);
        dataModel.put("properties", getProperties());

        //Multiple (Using Select Box DataList Filter and Process Assignment DataList Filter for the looping)
        dataModel.put("values", getValueSet(dataList, name, getPropertyString("defaultValue")));
        FormRowSet options = getStreamOptions()
                .collect(Collectors.toCollection(FormRowSet::new));

        String size=getPropertyString("size")+"px";

        dataModel.put("options", options);
        dataModel.put("multivalue", isMultivalue() ? "multiple" : "");
        dataModel.put("size", size);

        final String mode = getMode();
        if("multiple".equalsIgnoreCase(mode)) {
            return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), "/templates/odooSelectBoxDataListFilter.ftl", null);
        } else if("dateTime".equalsIgnoreCase(mode)) {
            return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), "/templates/odooDatetimeDataListFilter.ftl", null);
        }
        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), "/templates/odooTextFieldDataListFilterType.ftl", null);
    }

    public Stream<FormRow> getStreamOptions() {
        return Stream.concat(getOptions().stream(), getOptionsBinder().stream());
    }

    /**
     * Get property "options"
     *
     * @return
     */
    public FormRowSet getOptions() {
        return getPropertyGridOptions("options");
    }
    
    /**
     * Get property "optionsBinder"
     *
     * @return
     */
    public FormRowSet getOptionsBinder() {
        return getPropertyElementSelectOptions("optionsBinder");
    }

    /**
     * Get property with type 'grid' for options
     *
     * @param name
     * @return
     */
    @Nonnull
    public FormRowSet getPropertyGridOptions(String name) {
        return Optional.ofNullable((Object[])getProperty(name))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>)o)
                .map(m -> {
                    FormRow formRow = new FormRow();
                    formRow.setProperty(FormUtil.PROPERTY_VALUE, String.valueOf(m.get("value")));
                    formRow.setProperty(FormUtil.PROPERTY_LABEL, String.valueOf(m.get("label")));
                    return formRow;
                })
                .collect(Collectors.toCollection(FormRowSet::new));
    }

    /**
     * Get property with type 'elementselect' for options
     *
     * @param name
     * @return
     */
    @Nonnull
    public FormRowSet getPropertyElementSelectOptions(String name) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

        Map<String, Object> optionsBinder = (Map<String, Object>)getProperty(name);

        if(optionsBinder != null){
            String className = optionsBinder.get("className").toString();
            Plugin optionsBinderPlugins = pluginManager.getPlugin(className);
            if(optionsBinderPlugins != null && optionsBinder.get("properties") != null) {
                ((PropertyEditable) optionsBinderPlugins).setProperties((Map) optionsBinder.get("properties"));
                return ((FormAjaxOptionsBinder) optionsBinderPlugins).loadAjaxOptions(null);
            }
        }

        return new FormRowSet();
    }

    /**
     * Get property "multivalue"
     *
     * @return
     */
    private boolean isMultivalue() {
        return "true".equalsIgnoreCase(getPropertyString("multivalue"));
    }

    @Override
    public DataListFilterQueryObject getQueryObject(DataList dataList, String name) {
        final String mode = getMode();
        if("multiple".equalsIgnoreCase(mode)) {
            final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());
            
            if(getValue(dataList, name) == null) {
                return null;
            }

            final String [] value;
            final String operator;
            
            value = getValues(dataList, name);
            operator = "in";

            DataListFilterQueryObject filterQueryObject;

            if (value[0].equals(""))
            {
                filterQueryObject = new OdooFilterQueryObject(name, operator, "", OdooFilterQueryObject.DataType.STRING);
            }
            else
            {
                filterQueryObject = new OdooFilterQueryObject(name, operator, value, OdooFilterQueryObject.DataType.STRING);
            }

            LogUtil.info(getClassName(), "Length: " + value.length);

            return filterQueryObject;
        } else if("dateTime".equalsIgnoreCase(mode)) {
            final DataListFilterQueryObject queryObject = new DataListFilterQueryObject();

            final boolean singleValue = "true".equalsIgnoreCase(getPropertyString("singleValue"));

            String valueFrom, valueTo;
            final String defaultValue = AppUtil.processHashVariable(getPropertyString("defaultValue"), null, null, null);
            if(!defaultValue.isEmpty()) {
                // more likely it is called from plugin kecak-plugins-datalist-api
                String[] defaultValues = defaultValue.split(";");
                valueFrom = getValue(dataList, name + "-from", defaultValues.length < 1 ? null : defaultValues[0]);
                valueTo = singleValue ? valueFrom : getValue(dataList, name + "-to", defaultValues.length < 2 ? null : defaultValues[1]);
            } else {
                final Optional<String> optValues = Optional.ofNullable(getValue(dataList, name));
                if(optValues.isPresent()) {
                    String[] split = optValues.get().split(";");
                    valueFrom = Arrays.stream(split).findFirst().orElse("");
                    valueTo = Arrays.stream(split).skip(1).findFirst().orElse("");
                } else {
                    valueFrom = Optional.ofNullable(getValue(dataList, name + "_from")).orElse("");
                    valueTo = singleValue ? valueFrom : Optional.ofNullable(getValue(dataList, name + "_to")).orElse("");
                }
            }

            final boolean showTime = "true".equals(getPropertyString("showTime"));

            @Nonnull
            final String databaseDateFunction;
            boolean emptyFilter = false;
            if(valueFrom == null || valueFrom.isEmpty()) {
                valueFrom = "1970-01-01 00:00:00";
                databaseDateFunction = "";
                emptyFilter = true;
            } else {
                valueFrom = showTime ? valueFrom : valueFrom + " 00:00:00";
                databaseDateFunction = getPropertyString("databaseDateFunction");
                emptyFilter = false;
            }

            @Nonnull
            final String filterDateFunction;
            if (valueTo == null || valueTo.isEmpty()) {
                valueTo = "9999-12-31 23:59:59";
                filterDateFunction = "";
            } else {
                valueTo = showTime ? valueTo : valueTo + " 23:59:59";
                filterDateFunction = getPropertyString("filterDateFunction");
                emptyFilter = false;
            }

            if (dataList != null && dataList.getBinder() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("((");
                if(databaseDateFunction.isEmpty()) {
                    sb.append(String.format("CAST(%s AS date)", dataList.getBinder().getColumnName(name)));
                } else {
                    sb.append(databaseDateFunction.replaceAll("\\?", dataList.getBinder().getColumnName(name)));
                }

                sb.append(" BETWEEN ");

                if(filterDateFunction.isEmpty()) {
                    sb.append("CAST(? AS date) AND CAST(? AS date))");
                } else {
                    sb.append(String.format("%s AND %s)", filterDateFunction, filterDateFunction));
                }
                if(emptyFilter){
                    sb.append(" OR (" + String.format("CAST(%s AS date)", dataList.getBinder().getColumnName(name)) + " IS NULL)");
                }
                sb.append(")");
                queryObject.setQuery(sb.toString());
                queryObject.setValues(new String[]{valueFrom, valueTo});

                return queryObject;
            }
        } else if("".equalsIgnoreCase(mode)) {
            final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());

            if(getValue(dataList, name) == null) {
                return null;
            }

            final String value;
            final String operator;
            
            value = getValue(dataList, name);
            operator = "=";

            LogUtil.info(getClassName(), "Value: " + value);

            final DataListFilterQueryObject filterQueryObject = new OdooFilterQueryObject(name, operator, value, OdooFilterQueryObject.DataType.STRING);
            // LogUtil.info(getClassName(), "Query filter: " + );
            return filterQueryObject;
        }
        return null;
    }

    /**
     * Return values as set
     * @param datalist
     * @param name
     * @param defaultValue
     * @return
     */
    @Nonnull
    private Set<String> getValueSet(DataList datalist, String name, String defaultValue) {
        return Optional.ofNullable(getValues(datalist, name, defaultValue))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(s -> s.split(";"))
                .flatMap(Arrays::stream)
                .distinct()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    protected <T> Stream<T> repeat(T value, int n) {
        return IntStream.rangeClosed(1, n).limit(n).boxed().map(i -> value);
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

    // @Override
    // public Set<String> getOfflineStaticResources() {
    //     Set<String> urls = new HashSet<String>();
    //     String contextPath = AppUtil.getRequestContextPath();
    //     urls.add(contextPath + "/plugin/org.joget.apps.datalist.lib.TextFieldDataListFilterType/js/jquery.placeholder.min.js");
        
    //     return urls;
    // }
}
