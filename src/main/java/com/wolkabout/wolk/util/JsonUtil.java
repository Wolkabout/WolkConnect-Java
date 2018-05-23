package com.wolkabout.wolk.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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
            return new String(message.getPayload(), "UTF-8");
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
            return mapper.writeValueAsString(object).getBytes("UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize: " + object, e);
        }
    }
}
