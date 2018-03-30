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
import com.wolkabout.wolk.connectivity.InboundMessageDeserializer;
import com.wolkabout.wolk.connectivity.model.InboundMessage;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateCommand;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonSingleInboundMessageDeserializer implements InboundMessageDeserializer {

    private final Gson gson;

    public JsonSingleInboundMessageDeserializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(ActuatorCommand.class, new ActuatorCommandDeserializer())
                .registerTypeAdapter(FirmwareUpdateCommand.class, new FirmwareUpdateCommandDeserializer())
                .registerTypeAdapter(ConfigurationCommand.class, new ConfigurationCommandDeserializer())
                .create();
    }

    @Override
    public ActuatorCommand deserializeActuatorCommand(InboundMessage inboundMessage) {
        final String channel = inboundMessage.getChannel();
        final String reference = channel.substring(channel.lastIndexOf("/") + 1);

        final ActuatorCommand withoutReference = gson.fromJson(inboundMessage.getPayload(), ActuatorCommand.class);
        return new ActuatorCommand(withoutReference.getType(), withoutReference.getValue(), reference);
    }

    @Override
    public FirmwareUpdateCommand deserializeFirmwareUpdateCommand(InboundMessage inboundMessage) {
        return gson.fromJson(inboundMessage.getPayload(), FirmwareUpdateCommand.class);
    }

    @Override
    public ConfigurationCommand deserializeConfigurationCommand(InboundMessage inboundMessage) throws IllegalArgumentException {
        return gson.fromJson(inboundMessage.getPayload(), ConfigurationCommand.class);
    }

    private class ActuatorCommandDeserializer implements JsonDeserializer<ActuatorCommand> {

        @Override
        public ActuatorCommand deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();

            final JsonElement commandElement = jsonObject.get("command");
            final ActuatorCommand.CommandType commandType =
                    commandElement != null ?
                            ActuatorCommand.CommandType.valueOf(commandElement.getAsString()) :
                            ActuatorCommand.CommandType.UNKNOWN;

            final JsonElement valueElement = jsonObject.get("value");
            final String value = valueElement != null ? valueElement.getAsString() : "";

            return new ActuatorCommand(commandType, value, "");
        }
    }

    private class FirmwareUpdateCommandDeserializer implements JsonDeserializer<FirmwareUpdateCommand> {
        @Override
        public FirmwareUpdateCommand deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();

            final JsonElement commandElement = jsonObject.get("command");
            final FirmwareUpdateCommand.Type commandType =
                    commandElement != null ?
                            FirmwareUpdateCommand.Type.valueOf(commandElement.getAsString()) :
                            FirmwareUpdateCommand.Type.UNKNOWN;

            switch (commandType) {
                case FILE_UPLOAD:
                    final JsonElement fileNameElement = jsonObject.get("fileName");
                    final String fileName = fileNameElement.getAsString();

                    final JsonElement fileSizeElement = jsonObject.get("fileSize");
                    final long fileSize = fileSizeElement.getAsLong();

                    final JsonElement base64fileSha256Element = jsonObject.get("fileHash");
                    final String base64fileSha256 = base64fileSha256Element.getAsString();

                    final JsonElement autoInstallElement = jsonObject.get("autoInstall");
                    final boolean autoInstall = autoInstallElement.getAsBoolean();

                    return new FirmwareUpdateCommand(commandType, fileName, fileSize, base64fileSha256, autoInstall);

                case URL_DOWNLOAD:
                    final JsonElement fileUrlElement = jsonObject.get("fileUrl");
                    final String fileUrl = fileUrlElement.getAsString();

                    final JsonElement urlUutoInstallElement = jsonObject.get("autoInstall");
                    final boolean urlAutoInstall = urlUutoInstallElement.getAsBoolean();

                    return new FirmwareUpdateCommand(commandType, fileUrl, urlAutoInstall);

                default:
                    return new FirmwareUpdateCommand(commandType);
            }
        }
    }

    private class ConfigurationCommandDeserializer implements JsonDeserializer<ConfigurationCommand> {
        @Override
        public ConfigurationCommand deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();

            final JsonElement commandElement = jsonObject.get("command");
            final ConfigurationCommand.CommandType commandType = commandElement != null ?
                    ConfigurationCommand.CommandType.valueOf(commandElement.getAsString()) :
                    ConfigurationCommand.CommandType.UNKNOWN;

            if (commandType == ConfigurationCommand.CommandType.CURRENT) {
                return new ConfigurationCommand(commandType, new HashMap<String, String>());
            }

            final JsonElement valuesElement = jsonObject.get("values");
            final JsonObject valuesElementObject = valuesElement.getAsJsonObject();

            final Map<String, String> configurationValues = new HashMap<>();
            for (final Map.Entry<String, JsonElement> configurationItem : valuesElementObject.entrySet()) {
                configurationValues.put(configurationItem.getKey(), configurationItem.getValue().getAsString());
            }

            return new ConfigurationCommand(commandType, configurationValues);
        }
    }
}
