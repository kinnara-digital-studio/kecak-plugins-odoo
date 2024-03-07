package com.kinnarastudio.kecakplugins.odoo.common.property;

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
                    final String value = m.get("value");
                    return new SearchFilter(field, operator, value);
                })
                .toArray(SearchFilter[]::new);
    }
}
