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
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.util.WorkflowUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

public class OdooDataListFilter extends DataListFilterTypeDefault{
    public final static String LABEL = "Odoo DataList Filter";

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

        //Multiple (Using Select Box DataList Filter)
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
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        // final DataListFilterType filterPlugin = pluginManager.getPlugin(getFilterPlugin());

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

        final DataListFilterQueryObject filterQueryObject = new OdooFilterQueryObject(name, operator, value, OdooFilterQueryObject.DataType.STRING);
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
