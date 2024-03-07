package com.kinnarastudio.kecakplugins.odoo.common.property;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooUtilityException;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public final class OdooRpcToolUtil {
    public static String getMethod(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("method");
    }

    public static Optional<Integer> optRecordId(ExtDefaultPlugin plugin) {
        return Optional.of(plugin.getPropertyString("recordId"))
                .filter(Try.toNegate(String::isEmpty))
                .map(Try.onFunction(Integer::parseInt));
    }

    public static Map<String, Object> getRecord(ExtDefaultPlugin plugin) {
        return Optional.of(plugin.getPropertyGrid("record"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toMap(m -> String.valueOf(m.get("field")), m -> m.get("value")));
    }

    public static Map<String, String> getResultRecord(ExtDefaultPlugin plugin) {
        return Optional.of(plugin.getPropertyGrid("resultRecord"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toMap(m -> String.valueOf(m.get("field")), m -> m.get("wfVariable")));
    }

    public static String getResultWorkflowVariable(ExtDefaultPlugin plugin) {
        return plugin.getPropertyString("resultWfVariable");
    }

    public static Optional<Form> getResultForm(ExtDefaultPlugin plugin) {
        final String formDefId = plugin.getPropertyString("resultFormDefId");

        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final FormService formService = (FormService) appContext.getBean("formService");
        final FormDefinitionDao formDefinitionDao = (FormDefinitionDao)appContext.getBean("formDefinitionDao");

        final AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            final FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                Form form = (Form) formService.createElementFromJson(json);

                return Optional.ofNullable(form);
            }
        }

        return Optional.empty();
    }
}
