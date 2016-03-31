package com.wolkabout.wolk;

class Reading {

    private final ReadingType type;
    private final String value;

    Reading(final ReadingType type, final String value) {
        this.type = type;
        this.value = value;
    }

    ReadingType getType() {
        return type;
    }

    String getValue() {
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
