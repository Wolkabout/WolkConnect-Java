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
package com.wolkabout.wolk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolkabout.wolk.connectivity.InboundMessageDeserializer;
import com.wolkabout.wolk.connectivity.model.InboundMessage;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateCommand;

import java.io.IOException;

public class JsonSingleInboundMessageDeserializer implements InboundMessageDeserializer {
    @Override
    public ActuatorCommand deserializeActuatorCommand(InboundMessage inboundMessage) throws IllegalArgumentException {
        try {
            final String channel = inboundMessage.getChannel();
            final String reference = channel.substring(channel.lastIndexOf("/") + 1);

            final ActuatorCommand withoutReference = new ObjectMapper().readValue(inboundMessage.getPayload(), ActuatorCommand.class);
            return new ActuatorCommand(withoutReference.getType(), withoutReference.getValue(), reference);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public FirmwareUpdateCommand deserializeFirmwareUpdateCommand(InboundMessage inboundMessage) throws IllegalArgumentException {
        try {
            return new ObjectMapper().readValue(inboundMessage.getPayload(), FirmwareUpdateCommand.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
