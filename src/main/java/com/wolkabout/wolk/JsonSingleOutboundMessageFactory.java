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

import com.google.gson.*;
import com.wolkabout.wolk.connectivity.OutboundMessageFactory;
import com.wolkabout.wolk.connectivity.model.OutboundMessage;
import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateStatus;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

class JsonSingleOutboundMessageFactory implements OutboundMessageFactory {

    private static final String DELIMITER = ",";

    private static final String SENSOR_READINGS_CHANNEL = "readings/";
    private static final String ACTUATOR_STATUSES_CHANNEL = "actuators/status/";
    private static final String CURRENT_CONFIGURATION_CHANNEL = "configurations/current/";
    private static final String ALARMS_CHANNEL = "events/";

    private static final String FIRMWARE_UPDATE_STATUSES_CHANNEL = "service/status/firmware/";
    private static final String FIRMWARE_UPDATE_PACKET_REQUESTS_CHANNEL = "service/status/file/";
    private static final String FIRMWARE_VERSION_CHANNEL = "firmware/version/";

    private static final String KEEP_ALIVE_CHANNEL = "ping/";

    private final String deviceKey;

    private final Gson gson;

    JsonSingleOutboundMessageFactory(String deviceKey) {
        this.deviceKey = deviceKey;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(SensorReading.class, new SensorReadingSerializer())
                .registerTypeAdapter(Alarm.class, new AlarmSerializer())
                .registerTypeAdapter(ActuatorStatus.class, new ActuatorStatusSerializer())
                .registerTypeAdapter(FirmwareUpdateStatus.class, new FirmwareUpdateStatusSerializer())
                .registerTypeAdapter(FileTransferPacketRequest.class, new FileTransferPacketRequestSerializer())
                .create();
    }

    @Override
    public OutboundMessage makeFromSensorReadings(List<SensorReading> readings) {
        final String payload = gson.toJson(readings);
        final String channel = SENSOR_READINGS_CHANNEL + deviceKey + "/" + readings.get(0).getReference();
        return new OutboundMessage(payload, channel, readings.size());
    }

    @Override
    public OutboundMessage makeFromActuatorStatuses(List<ActuatorStatus> actuatorStatuses) {
        if (actuatorStatuses.isEmpty()) {
            throw new IllegalArgumentException("Empty actuator statuses collection received.");
        }

        final String payload = gson.toJson(actuatorStatuses.get(0));
        final String channel = ACTUATOR_STATUSES_CHANNEL + deviceKey + "/" + actuatorStatuses.get(0).getReference();
        return new OutboundMessage(payload, channel, 1);
    }

    @Override
    public OutboundMessage makeFromAlarms(List<Alarm> alarms) {
        final String payload = gson.toJson(alarms);
        final String channel = ALARMS_CHANNEL + deviceKey + "/" + alarms.get(0).getReference();
        return new OutboundMessage(payload, channel, alarms.size());
    }

    @Override
    public OutboundMessage makeFromFirmwareUpdateStatus(FirmwareUpdateStatus firmwareUpdateStatus) {
        final String payload = gson.toJson(firmwareUpdateStatus);
        final String channel = FIRMWARE_UPDATE_STATUSES_CHANNEL + deviceKey;
        return new OutboundMessage(payload, channel);
    }

    @Override
    public OutboundMessage makeFromFileTransferPacketRequest(FileTransferPacketRequest fileTransferPacketRequest) {
        final String payload = gson.toJson(fileTransferPacketRequest);
        final String channel = FIRMWARE_UPDATE_PACKET_REQUESTS_CHANNEL + deviceKey;
        return new OutboundMessage(payload, channel);
    }

    @Override
    public OutboundMessage makeFromFirmwareVersion(String firmwareVersion) {
        final String channel = FIRMWARE_VERSION_CHANNEL + deviceKey;
        return new OutboundMessage(firmwareVersion, channel);
    }

    @Override
    public OutboundMessage makeFromConfiguration(Map<String, String> configuration) throws IllegalArgumentException {
        final String channel = CURRENT_CONFIGURATION_CHANNEL + deviceKey;

        final JsonObject result = new JsonObject();
        result.add("values", gson.toJsonTree(configuration));
        final String payload = result.toString();

        return new OutboundMessage(payload, channel);
    }

    @Override
    public OutboundMessage makeFromKeepAliveMessage() {
        final String channel = KEEP_ALIVE_CHANNEL + deviceKey;
        return new OutboundMessage("", channel);
    }

    private class SensorReadingSerializer implements JsonSerializer<SensorReading> {

        @Override
        public JsonElement serialize(SensorReading sensorReading, Type type, JsonSerializationContext context) {
            if (sensorReading.getValues().isEmpty()) {
                throw new IllegalArgumentException("Given sensor reading does not have reading data.");
            }

            final JsonObject result = new JsonObject();
            result.add("utc", new JsonPrimitive(sensorReading.getUtc()));

            final List<String> sensorReadingValues = sensorReading.getValues();

            if (sensorReadingValues.size() == 1) {
                result.add("data", new JsonPrimitive(sensorReadingValues.get(0)));
            } else {
                final StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < sensorReadingValues.size(); ++i) {
                    if (i != 0) {
                        stringBuilder.append(DELIMITER);
                    }

                    stringBuilder.append(sensorReadingValues.get(i));
                }

                result.add("data", new JsonPrimitive(stringBuilder.toString()));
            }

            return result;
        }
    }

    private class AlarmSerializer implements JsonSerializer<Alarm> {

        @Override
        public JsonElement serialize(Alarm alarm, Type type, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            result.add("utc", new JsonPrimitive(alarm.getUtc()));
            result.add("data", new JsonPrimitive(alarm.getValue()));
            return result;
        }
    }

    private class ActuatorStatusSerializer implements JsonSerializer<ActuatorStatus> {

        @Override
        public JsonElement serialize(ActuatorStatus actuatorStatus, Type type, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            result.add("status", new JsonPrimitive(String.valueOf(actuatorStatus.getStatus())));
            result.add("value", new JsonPrimitive(actuatorStatus.getValue()));
            return result;
        }
    }

    private class FirmwareUpdateStatusSerializer implements JsonSerializer<FirmwareUpdateStatus> {

        @Override
        public JsonElement serialize(FirmwareUpdateStatus firmwareUpdateStatus, Type type, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            result.add("status", new JsonPrimitive(String.valueOf(firmwareUpdateStatus.getStatus())));
            if (firmwareUpdateStatus.getErrorCode() != null) {
                result.add("error", new JsonPrimitive(firmwareUpdateStatus.getErrorCode()));
            }
            return result;
        }
    }

    private class FileTransferPacketRequestSerializer implements JsonSerializer<FileTransferPacketRequest> {

        @Override
        public JsonElement serialize(FileTransferPacketRequest fileTransferPacketRequest, Type type, JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            result.add("fileName", new JsonPrimitive(String.valueOf(fileTransferPacketRequest.getFileName())));
            result.add("chunkIndex", new JsonPrimitive(String.valueOf(fileTransferPacketRequest.getPacketId())));
            result.add("chunkSize", new JsonPrimitive(String.valueOf(fileTransferPacketRequest.getPacketSize())));
            return result;
        }
    }
}
