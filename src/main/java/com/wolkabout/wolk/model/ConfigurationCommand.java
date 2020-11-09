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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class ConfigurationCommand {

    public enum CommandType {
        UNKNOWN, SET
    }

    private CommandType command;
    @JsonProperty
    private Map<String, Object> values;

    @JsonIgnore
    private final Collection<Configuration> parsedValues = new HashSet<>();

    public ConfigurationCommand() {
    }

    public ConfigurationCommand(CommandType commandType, Map<String, Object> values) {
        this.command = commandType;
        setValues(values);
    }

    public CommandType getType() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    @JsonIgnore
    public Collection<Configuration> getValues() {
        return parsedValues;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            parsedValues.add(new Configuration(entry.getKey(), entry.getValue().toString()));
        }
    }

    @Override
    public String toString() {
        return "ConfigurationCommand{" +
                "commandType='" + command + '\'' +
                ", values='" + values + '\'' +
                '}';
    }
}
