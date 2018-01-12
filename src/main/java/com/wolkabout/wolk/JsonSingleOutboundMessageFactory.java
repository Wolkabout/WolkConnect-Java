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
import com.wolkabout.wolk.connectivity.OutboundMessageFactory;
import com.wolkabout.wolk.connectivity.model.OutboundMessage;
import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateStatus;

import java.util.List;

class JsonSingleOutboundMessageFactory implements OutboundMessageFactory {
    private final String deviceKey;

    JsonSingleOutboundMessageFactory(final String deviceKey) {
        this.deviceKey = deviceKey;
    }

    @Override
    public OutboundMessage makeFromReadings(List<SensorReading> readings) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(readings);
            final String channel = "readings/" + deviceKey + "/" + readings.get(0).getReference();
            return new OutboundMessage(payload, channel, readings.size());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromActuatorStatuses(List<ActuatorStatus> actuatorStatuses) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(actuatorStatuses.get(0));
            final String channel = "actuators/status/" + deviceKey + "/" + actuatorStatuses.get(0).getReference();
            return new OutboundMessage(payload, channel, 1);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromAlarms(List<Alarm> alarms) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(alarms);
            final String channel = "events/" + deviceKey + "/" + alarms.get(0).getReference();
            return new OutboundMessage(payload, channel, alarms.size());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromFirmwareUpdateStatus(FirmwareUpdateStatus status) throws IllegalArgumentException {
        try {
        final String payload = new ObjectMapper().writeValueAsString(status);
        final String channel = "service/status/firmware/" + deviceKey;
        return new OutboundMessage(payload, channel);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromFileTransferPacketRequest(FileTransferPacketRequest request) throws IllegalArgumentException {
        try {
            final String payload = new ObjectMapper().writeValueAsString(request);
            final String channel = "service/status/file/" + deviceKey;
            return new OutboundMessage(payload, channel);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public OutboundMessage makeFromFirmwareVersion(String firmwareVersion) throws IllegalArgumentException {
        final String channel = "firmware/version/" + deviceKey;
        return new OutboundMessage(firmwareVersion, channel);
    }
}
