package com.wolkabout.wolk.model;

public class Attribute {
    private final String name;
    private final DataType dataType;
    private final String value;

    public Attribute(String name, DataType dataType, String value) {
        this.name = name;
        this.dataType = dataType;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", dataType=" + dataType +
                ", value=" + value +
                '}';
    }
}
