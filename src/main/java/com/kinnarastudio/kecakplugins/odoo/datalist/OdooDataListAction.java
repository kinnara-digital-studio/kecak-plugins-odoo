package com.kinnarastudio.kecakplugins.odoo.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.odoo.common.property.OdooDataListActionUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.ResourceBundle;

public class OdooDataListAction extends DataListActionDefault {
    public final static String LABEL = "Odoo DataList Action";

    @Override
    public String getLinkLabel() {
        return OdooDataListActionUtil.getLabel(this);
    }

    @Override
    public String getHref() {
        return OdooDataListActionUtil.getHref(this);
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return "";
    }

    @Override
    public String getHrefColumn() {
        return "";
    }

    @Override
    public String getConfirmation() {
        return OdooDataListActionUtil.getConfirmation(this);
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        return new DataListActionResult() {{
            setType(DataListActionResult.TYPE_REDIRECT);
        }};
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
        final String[] resources = new String[]{
                "/properties/common/OdooAuthorization.json",
                "/properties/datalist/OdooDataListAction.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
    }
}
