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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

class JsonSingleOutboundMessageFactory implements OutboundMessageFactory {
    private final String deviceKey;

    JsonSingleOutboundMessageFactory(final String deviceKey) {
        this.deviceKey = deviceKey;
    }

    @Override
    public OutboundMessage makeFromReadings(List<Reading> readings) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(readings);
            final String topic = "readings/" + deviceKey + "/" + readings.get(0).getReference();
            return new OutboundMessage(payload, topic, readings.size());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromActuatorStatuses(List<ActuatorStatus> actuatorStatuses) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(actuatorStatuses.get(0));
            final String topic = "actuators/status/" + deviceKey + "/" + actuatorStatuses.get(0).getRef();
            return new OutboundMessage(payload, topic, 1);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
