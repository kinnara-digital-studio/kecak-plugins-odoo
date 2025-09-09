package com.kinnarastudio.kecakplugins.odoo.form;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;

import java.util.*;

/**
 * Copy from {@link org.joget.apps.form.lib.WorkflowFormBinder} but using
 * {@link OdooFormBinder} instead of {@link DefaultFormBinder}
 */
public class OdooWorkflowFormBinder extends OdooFormBinder implements FormLoadElementBinder, FormStoreElementBinder {
    public final static String LABEL = "Odoo Workflow Form Binder";

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet rows = super.load(element, primaryKey, formData);

        if (rows != null) {
            // handle workflow variables
            String activityId = formData.getActivityId();
            String processId = formData.getProcessId();
            WorkflowManager workflowManager = (WorkflowManager) WorkflowUtil.getApplicationContext().getBean("workflowManager");
            Collection<WorkflowVariable> variableList;
            if (activityId != null && !activityId.isEmpty()) {
                variableList = workflowManager.getActivityVariableList(activityId);
            } else if (processId != null && !processId.isEmpty()) {
                variableList = workflowManager.getProcessVariableList(processId);
            } else {
                variableList = new ArrayList<>();
            }

            if (variableList != null && !variableList.isEmpty()) {
                FormRow row;
                if (rows.isEmpty()) {
                    row = new FormRow();
                    rows.add(row);
                } else {
                    row = rows.iterator().next();
                }

                Map<String, String> variableMap = new HashMap<>();
                for (WorkflowVariable variable : variableList) {
                    Object val = variable.getVal();
                    if (val != null) {
                        variableMap.put(variable.getId(), val.toString());
                    }
                }
                loadWorkflowVariables(element, row, variableMap);
            }
        }
        return rows;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        FormRowSet result = rows;
        if (rows != null && !rows.isEmpty()) {
            // store form data to DB
            result = super.store(element, rows, formData);

            // handle workflow variables
            if (!rows.isMultiRow()) {
                String activityId = formData.getActivityId();
                String processId = formData.getProcessId();
                if (activityId != null || processId != null) {
                    WorkflowManager workflowManager = (WorkflowManager) WorkflowUtil.getApplicationContext().getBean("workflowManager");

                    // recursively find element(s) mapped to workflow variable
                    FormRow row = rows.iterator().next();
                    formData.setPrimaryKeyValue(row.getId());

                    Map<String, String> variableMap = new HashMap<>();
                    variableMap = storeWorkflowVariables(element, row, variableMap);

                    if (activityId != null) {
                        workflowManager.activityVariables(activityId, variableMap);
                    } else {
                        workflowManager.processVariables(processId, variableMap);
                    }
                }
            }
        }
        return result;
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
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return OdooWorkflowFormBinder.class.getName();
    }

    /**
     * Recursive into elements to set workflow variable values to be loaded.
     *
     * @param element
     * @param row         The current row of data to be loaded
     * @param variableMap The variable name=value pairs.
     * @return
     */
    protected Map<String, String> loadWorkflowVariables(Element element, FormRow row, Map<String, String> variableMap) {
        String variableName = element.getPropertyString(AppUtil.PROPERTY_WORKFLOW_VARIABLE);
        if (variableName != null && !variableName.trim().isEmpty()) {
            String id = element.getPropertyString(FormUtil.PROPERTY_ID);
            String variableValue = variableMap.get(variableName);
            if (variableValue != null) {
                row.put(id, variableValue);
            }
        }
        for (Iterator<Element> i = element.getChildren().iterator(); i.hasNext(); ) {
            Element child = i.next();
            loadWorkflowVariables(child, row, variableMap);
        }
        return variableMap;
    }

    /**
     * Recursive into elements to retrieve workflow variable values to be stored.
     *
     * @param element
     * @param row         The current row of data
     * @param variableMap The variable name=value pairs to be stored.
     * @return
     */
    protected Map<String, String> storeWorkflowVariables(Element element, FormRow row, Map<String, String> variableMap) {
        String variableName = element.getPropertyString(AppUtil.PROPERTY_WORKFLOW_VARIABLE);
        if (variableName != null && !variableName.trim().isEmpty()) {
            String id = element.getPropertyString(FormUtil.PROPERTY_ID);
            String value = String.valueOf(row.get(id));
            if (value != null) {
                variableMap.put(variableName, value);
            }
        }
        for (Iterator<Element> i = element.getChildren().iterator(); i.hasNext(); ) {
            Element child = i.next();
            storeWorkflowVariables(child, row, variableMap);
        }
        return variableMap;
    }
}