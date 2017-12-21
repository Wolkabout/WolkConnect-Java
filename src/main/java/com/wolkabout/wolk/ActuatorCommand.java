/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActuatorCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ActuatorCommand.class);

    public enum CommandType {SET, STATUS, UNKNOWN}

    @JsonProperty(value = "command")
    private String commandType;

    @JsonProperty(value = "value")
    private String value;

    @JsonIgnore
    private String reference;

    // Required by Jackson
    private ActuatorCommand() {
    }

    public ActuatorCommand(CommandType commandType, String value, String reference) {
        this.commandType = commandType.toString();
        this.value = value;
        this.reference = reference;
    }

    public CommandType getType() {
        try {
            return CommandType.valueOf(commandType);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown command: {}", commandType);
            return CommandType.UNKNOWN;
        }
    }

    public String getValue() {
        return value;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "ActuatorCommand{" +
                "type='" + commandType + '\'' +
                ", value='" + value + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
