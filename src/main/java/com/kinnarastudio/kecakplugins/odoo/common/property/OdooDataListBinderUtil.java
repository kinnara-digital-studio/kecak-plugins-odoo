package com.kinnarastudio.kecakplugins.odoo.common.property;

import com.kinnarastudio.odooxmlrpc.model.DataType;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import org.joget.plugin.base.ExtDefaultPlugin;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

public final class OdooDataListBinderUtil {
    private OdooDataListBinderUtil() {}

    @Nonnull
    public static SearchFilter[] getFilter(ExtDefaultPlugin plugin) {
        return Arrays.stream(plugin.getPropertyGrid("filter"))
                .map(m -> {
                    final String field = m.get("field");
                    final SearchFilter.Operator operator = SearchFilter.Operator.parse(m.get("operator"));
                    final DataType dataType = "integer".equalsIgnoreCase(m.get("dataType")) ? DataType.INTEGER : DataType.STRING;
                    final String strValue = String.valueOf(m.get("value"));

                    Object value;
                    if (SearchFilter.Operator.IN == operator) {
                        value = Arrays.stream(strValue.split(";"))
                                .map(String::trim)
                                .map(s -> {
                                    if (dataType == DataType.INTEGER) {
                                        try {
                                            return Integer.parseInt(s);
                                        } catch (NumberFormatException e) {
                                            return null;
                                        }
                                    }
                                    return s;
                                })
                                .filter(Objects::nonNull)
                                .toArray();
                    } else {
                        try {
                            value = dataType == DataType.INTEGER ? Integer.parseInt(strValue) : strValue;
                        } catch (NumberFormatException e) {
                            value = strValue;
                        }
                    }

                    return new SearchFilter(field, operator, value);
                })
                .toArray(SearchFilter[]::new);
    }
}
