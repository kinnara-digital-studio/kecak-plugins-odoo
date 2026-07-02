package com.kinnarastudio.kecakplugins.odoo.common.property;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import org.joget.plugin.base.ExtDefaultPlugin;

public final class OdooDataListBinderUtil {
    private OdooDataListBinderUtil() {
    }

    @Nonnull
    public static SearchFilter[] getFilter(ExtDefaultPlugin plugin) {
        return Arrays.stream(plugin.getPropertyGrid("filter"))
                .map(m -> {
                    final String field = m.get("field");
                    final SearchFilter.Operator operator = SearchFilter.Operator.parse(m.get("operator"));
                    final DataType dataType = DataType.parse(m.get("dataType"));
                    final String strValue = String.valueOf(m.get("value"));

                    Object value;
                    if (SearchFilter.Operator.IN == operator) {
                        value = Arrays.stream(strValue.split(";"))
                                .map(String::trim)
                                .map(dataType::valueParser)
                                .filter(Objects::nonNull)
                                .toArray();
                    } else {
                        try {
                            value = dataType.valueParser(strValue);
                        } catch (NumberFormatException e) {
                            value = "null".equalsIgnoreCase(strValue) ? null : strValue;
                        }
                    }

                    final SearchFilter.Join join = SearchFilter.Join.parse(m.getOrDefault("join", SearchFilter.Join.AND.name()));

                    return new SearchFilter(join, field, operator, value);
                })
                .toArray(SearchFilter[]::new);
    }

}
