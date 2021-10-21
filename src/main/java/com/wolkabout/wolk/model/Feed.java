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

    private final String reference;

    private final List<Object> values;

    private final long utc;

    public Feed(String reference, Object value) {
        this(reference, Collections.singletonList(value), System.currentTimeMillis());
    }

    public Feed(String reference, Object value, long utc) {
        this(reference, Collections.singletonList(value), utc);
    }

    public Feed(String reference, List<Object> values) {
        this(reference, values, System.currentTimeMillis());
    }

    public Feed(String reference, List<Object> values, long utc) {
        this.reference = reference;
        this.values = values;
        this.utc = utc;
    }

    public String getReference() {
        return reference;
    }

    public List<Object> getValues() {
        return values;
    }

    public Object getValue() {
        return values.get(0);
    }

    public List<String> getStringValues() {
        return values.stream().map(Object::toString).collect(Collectors.toList());
    }

    public String getStringValue() {
        return values.get(0).toString();
    }

    public List<Double> getNumericValues() {
        return values.stream().map(this::toDouble).collect(Collectors.toList());
    }

    public Double getNumericValue() {
        return toDouble(values.get(0));
    }

    public List<Boolean> getBooleanValues() {
        return values.stream().map(this::toBool).collect(Collectors.toList());
    }

    public Boolean getBooleanValue() {
        return toBool(values.get(0));
    }

    public long getUtc() {
        return utc;
    }

    private Double toDouble(Object obj) {
        try {
            return (Double) obj;
        } catch (ClassCastException e) {
            return Double.parseDouble(obj.toString());
        }
    }

    private Boolean toBool(Object obj) {
        try {
            return (Boolean) obj;
        } catch (ClassCastException e) {
            return Boolean.parseBoolean(obj.toString());
        }
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
