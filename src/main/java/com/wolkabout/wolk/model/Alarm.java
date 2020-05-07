package com.wolkabout.wolk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Alarm {
    @JsonIgnore
    private final String reference;
    private final boolean active;
    private final String code;
    private final Long utc;

    public Alarm(String reference, boolean active, String code) {
        this(reference, active, code, System.currentTimeMillis());
    }

    public Alarm(String reference, boolean active, String code, Long utc) {
        this.reference = reference;
        this.active = active;
        this.code = code;
        this.utc = utc;
    }

    public String getReference() {
        return reference;
    }

    public String getActive() {
        return String.valueOf(active);
    }

    public String getCode() {
        return code;
    }

    public long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "reference='" + reference + '\'' +
                ", active=" + active + '\'' +
                ", code='" + code + '\'' +
                ", utc=" + utc +
                '}';
    }


}
