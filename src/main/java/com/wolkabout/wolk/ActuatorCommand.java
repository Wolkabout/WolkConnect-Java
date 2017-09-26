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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActuatorCommand {
    private Logger logger = LoggerFactory.getLogger(Wolk.class);

    public enum Command {SET, STATUS, UNKOWN}

    private String command;
    private String value;

    public Command getCommand() {
        try {
            return Command.valueOf(this.command);
        } catch (IllegalArgumentException e) {
            logger.warn("Unkonwn command: " + command);
            return Command.UNKOWN;
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ActuatorCommand{" + "command='" + command + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
