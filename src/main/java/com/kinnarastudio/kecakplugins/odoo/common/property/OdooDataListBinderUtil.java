package com.kinnarastudio.kecakplugins.odoo.common.property;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.IOdooFilter;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import org.joget.plugin.base.ExtDefaultPlugin;

import javax.annotation.Nonnull;
import java.util.Arrays;

public final class OdooDataListBinderUtil {
    private OdooDataListBinderUtil() {}

    @Nonnull
    public static SearchFilter[] getFilter(ExtDefaultPlugin plugin) {
        return Arrays.stream(plugin.getPropertyGrid("filter"))
                .map(m -> {
                    final String field = m.get("field");
                    final String operator = m.get("operator");
                    final IOdooFilter.DataType dataType = "integer".equalsIgnoreCase(m.get("dataType")) ? IOdooFilter.DataType.INTEGER : IOdooFilter.DataType.STRING;
                    final String strValue = String.valueOf(m.get("value"));

                    Object value;
                    try {
                        value = dataType == IOdooFilter.DataType.INTEGER ? Integer.parseInt(strValue) : strValue;
                    } catch (NumberFormatException e) {
                        value = strValue;
                    }

                    return new SearchFilter(field, operator, value);
                })
                .toArray(SearchFilter[]::new);
    }
}
