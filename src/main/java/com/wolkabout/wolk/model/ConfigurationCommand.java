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

import java.util.Map;

public class ConfigurationCommand {

    public enum CommandType {
        UNKNOWN, SET, CURRENT
    }

    private CommandType command;
    private Map<String, Object> values;

    public ConfigurationCommand() {}

    public ConfigurationCommand(CommandType commandType, Map<String, Object> values) {
        this.command = commandType;
        this.values = values;
    }

    public CommandType getType() {
       return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) { this.values = values; }

    @Override
    public String toString() {
        return "ConfigurationCommand{" +
                "commandType='" + command + '\'' +
                ", values='" + values + '\'' +
                '}';
    }
}
