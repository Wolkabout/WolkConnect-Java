package com.wolkabout.wolk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Alarm {
    @JsonIgnore
    private final String reference;
    private final boolean active;
    private final Long utc;

    public Alarm(String reference, boolean active) {
        this(reference, active, System.currentTimeMillis());
    }

    public Alarm(String reference, boolean active, Long utc) {
        this.reference = reference;
        this.active = active;
        this.utc = utc;
    }

    public String getReference() {
        return reference;
    }

    public String getActive() {
        return String.valueOf(active);
    }

    public long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "reference='" + reference + '\'' +
                ", active=" + active + '\'' +
                ", utc=" + utc +
                '}';
    }


}
