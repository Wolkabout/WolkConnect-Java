/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.wolkabout.wolk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wolkabout.wolk.util.JsonMultivalueSerializer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Feed {

    @JsonIgnore
    private final String reference;

    @JsonProperty("data")
    @JsonSerialize(using = JsonMultivalueSerializer.class)
    private final List<String> values;

    private final long utc;

    public Feed(String reference, String value) {
        this(reference, Collections.singletonList(value), System.currentTimeMillis());
    }

    public Feed(String reference, String value, long utc) {
        this(reference, Collections.singletonList(value), utc);
    }

    public Feed(String reference, List<String> values) {
        this(reference, values, System.currentTimeMillis());
    }

    public Feed(String reference, List<String> values, long utc) {
        this.reference = reference;
        this.values = values;
        this.utc = utc;
    }

    public String getReference() {
        return reference;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue() {
        return values.get(0);
    }

    public List<Double> getNumericValues() {
        return values.stream().map(Double::parseDouble).collect(Collectors.toList());
    }

    public Double getNumericValue() {
        return Double.parseDouble(values.get(0));
    }

    public List<Boolean> getBooleanValues() {
        return values.stream().map(Boolean::parseBoolean).collect(Collectors.toList());
    }

    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(values.get(0));
    }

    public long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Feed{" +
                "reference='" + reference + '\'' +
                ", values=" + values +
                ", utc=" + utc +
                '}';
    }
}
