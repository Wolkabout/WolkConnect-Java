package com.wolkabout.wolk.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolkabout.wolk.model.*;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonSingleReferenceProtocol extends Protocol {

    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonSingleReferenceProtocol(MqttClient client) {
        super(client);
    }

    @Override
    protected String getActuationTopic() {
        return "actuators/commands/";
    }

    @Override
    protected String getConfigurationTopic() {
        return "configurations/commands/";
    }

    @Override
    protected ActuatorCommand parseActuatorCommand(String payload) {
        try {
            return mapper.readValue(payload, ActuatorCommand.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize actuator command: " + payload);
        }
    }

    @Override
    protected Map<String, Object> parseConfiguration(String payload) {
        try {
            return mapper.readValue(payload, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize actuator command: " + payload);
        }
    }

    @Override
    public void publish(Reading reading) {
        publish("readings/" + client.getClientId() + "/" + reading.getRef(), new DataWrapper(reading.getValue()));
    }

    @Override
    public void publish(List<Reading> readings) {
        for (Reading reading : readings) {
            publish(reading);
        }
    }

    @Override
    public void publish(ConfigurationCommand configurations) {
        publish("configurations/current", configurations);
    }

    @Override
    public void publish(ActuatorStatus actuatorStatus) {
        publish("actuators/status", actuatorStatus);
    }

    private void publish(String topic, Object payload) {
        try {
            client.publish("readings/" + client.getClientId(), serialize(payload), 1, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    private byte[] serialize(Object object) {
        try {
            return mapper.writeValueAsString(object).getBytes("UTF-8");
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not deserialize: " + object, e);
        }
    }
}
