package com.wolkabout.wolk;

class Reading {

    private final ReadingType type;
    private final String value;

    public Reading(final ReadingType type, final String value) {
        this.type = type;
        this.value = value;
    }

    public ReadingType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Reading{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}
