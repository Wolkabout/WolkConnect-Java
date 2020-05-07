/*
 * Copyright (c) 2019 WolkAbout Technology s.r.o.
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

import com.wolkabout.wolk.util.JsonMultivalueSerializer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Configuration {

    public enum Status {
        READY, BUSY, ERROR
    }

    private final String reference;
    private final Status status;
    private final List<String> values;
    private final String value;
    private final Long utc;

    public Configuration(String reference, Status status, String value) {
        this.reference = reference;
        this.status = status;
        this.value = value;
        this.values = JsonMultivalueSerializer.valuesFromString(value);
        this.utc = System.currentTimeMillis();
    }

    public Configuration(String reference, Status status, String value, Long utc) {
        this.reference = reference;
        this.status = status;
        this.value = value;
        this.values = JsonMultivalueSerializer.valuesFromString(value);
        this.utc = utc;
    }

    public Configuration(String reference, Status status, List<String> values) {
        this.reference = reference;
        this.status = status;
        this.value = JsonMultivalueSerializer.valuesToString(values);
        this.values = values;
        this.utc = System.currentTimeMillis();
    }

    public Configuration(String reference, Status status, List<String> values, Long utc) {
        this.reference = reference;
        this.status = status;
        this.value = JsonMultivalueSerializer.valuesToString(values);
        this.values = values;
        this.utc = utc;
    }

    public String getReference() {
        return reference;
    }

    public Status getStatus() { return status; }

    public List<String> getValues() {
        return values;
    }

    public String getValue() {
        return value;
    }

    public Long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "reference='" + reference + '\'' +
                ", status='" + status + '\'' +
                ", value='" + value + '\'' +
                ", utc=" + utc +
                '}';
    }
}