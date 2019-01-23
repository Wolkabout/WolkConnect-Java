package com.wolkabout.wolk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Alarm {
    @JsonIgnore
    private final String ref;

    @JsonProperty("data")
    private final boolean value;

    private final long utc;

    public Alarm(String ref, boolean value) {
        this(ref, value, System.currentTimeMillis());
    }

    public Alarm(String ref, boolean value, long utc) {
        this.ref = ref;
        this.value = value;
        this.utc = utc;
    }

    public String getReference() {
        return ref;
    }

    public String getValue() {
        return String.valueOf(value);
    }

    public long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "ref='" + ref + '\'' +
                ", value=" + value +
                ", utc=" + utc +
                '}';
    }
}
