package com.kinnarastudio.kecakplugins.odoo.common.rpc;

import javax.annotation.Nonnull;
import java.util.Map;

public class Field {
    private final String key;
    private final String string;
    private final boolean required;
    private final DataType type;
    private final boolean sortable;
    private final String help;
    private final Map<String, Object> metadata;

    public Field(String key, Map<String, Object> metadata) {
        this.key = key;
        this.string = (String) metadata.get("string");
        this.required = "true".equals(metadata.getOrDefault("required", true));
        this.type = DataType.parse(String.valueOf(metadata.get("type")));
        this.sortable = "true".equals(metadata.getOrDefault("sortable", true));
        this.help = (String) metadata.get("help");
        this.metadata = metadata;
    }

    public String getKey() {
        return key;
    }

    public String getString() {
        return string;
    }

    public boolean isRequired() {
        return required;
    }

    public DataType getType() {
        return type;
    }

    public boolean isSortable() {
        return sortable;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getHelp() {
        return help;
    }
}
