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

import java.util.List;

public class Configuration {


    private final String reference;
    private final List<String> values;
    private final String value;

    public Configuration(String reference, String value) {
        this.reference = reference;
        this.value = value;
        this.values = JsonMultivalueSerializer.valuesFromString(value);
    }

    public Configuration(String reference, String value, Long utc) {
        this.reference = reference;
        this.value = value;
        this.values = JsonMultivalueSerializer.valuesFromString(value);
    }

    public Configuration(String reference, List<String> values) {
        this.reference = reference;
        this.value = JsonMultivalueSerializer.valuesToString(values);
        this.values = values;
    }

    public String getReference() {
        return reference;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "reference='" + reference + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}