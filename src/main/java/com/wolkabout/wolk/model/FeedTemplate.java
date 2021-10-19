package com.wolkabout.wolk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeedTemplate {

    private final String name;
    private final FeedType type;
    private final String unitGuid;
    private final String reference;

    public FeedTemplate(String name, FeedType type, String unitGuid, String reference) {
        this.name = name;
        this.type = type;
        this.unitGuid = unitGuid;
        this.reference = reference;
    }

    public FeedTemplate(String name, FeedType type, Unit unitGuid, String reference) {
        this(name, type, unitGuid.name(), reference);
    }

    public String getName() {
        return name;
    }

    public FeedType getType() {
        return type;
    }

    public String getUnitGuid() {
        return unitGuid;
    }

    public String getReference() {
        return reference;
    }
}
