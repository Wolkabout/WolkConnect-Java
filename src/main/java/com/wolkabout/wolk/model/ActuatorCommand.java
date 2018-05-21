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

public class ActuatorCommand {

    public enum CommandType {
        UNKNOWN, SET, STATUS
    }

    private CommandType commandType;
    private String value;
    private String reference;

    public ActuatorCommand() {
    }

    public ActuatorCommand(CommandType commandType, String value, String reference) {
        this.commandType = commandType;
        this.value = value;
        this.reference = reference;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "ActuatorCommand{" +
                "commandType=" + commandType +
                ", value='" + value + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
