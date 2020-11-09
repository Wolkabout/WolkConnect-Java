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
package com.wolkabout.wolk.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {
        // Not meant to be instantiated.
    }

    public static <T> T deserialize(MqttMessage message, Class<T> type) {
        final String payload = extractPayload(message);
        return deserialize(payload, type);
    }

    private static String extractPayload(MqttMessage message) {
        try {
            return new String(message.getPayload(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to extract payload from: " + message, e);
        }
    }

    public static <T> T deserialize(String payload, Class<T> type) {
        try {
            return mapper.readValue(payload, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize " + type.getName() + " from " + payload, e);
        }
    }

    public static byte[] serialize(Object object) {
        try {
            return mapper.writeValueAsString(object).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize: " + object, e);
        }
    }
}
