package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Collection;
import java.util.Map;

public class JsonSingleReferenceProtocol extends Protocol {

    public JsonSingleReferenceProtocol(MqttClient client, ProtocolHandler handler) {
        super(client, handler);
    }

    @Override
    protected void subscribe() throws Exception {
        client.subscribe("actuators/commands/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final ActuatorCommand actuatorCommand = JsonUtil.deserialize(payload, ActuatorCommand.class);
                if (actuatorCommand.getCommandType() == ActuatorCommand.CommandType.SET) {
                    handler.onActuationReceived(actuatorCommand);
                } else {
                    final ActuatorStatus actuatorStatus = handler.getActuatorStatus(actuatorCommand.getReference());
                    publish(actuatorStatus);
                }
            }
        });

        client.subscribe("configurations/commands/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final Map<String, Object> configuration = JsonUtil.deserialize(payload, Map.class);
                handler.onConfigurationReceived(configuration);
            }
        });
    }

    @Override
    public void publish(Reading reading) {
        publish("readings/" + client.getClientId() + "/" + reading.getRef(), new DataWrapper(reading.getValue()));
    }

    @Override
    public void publish(Collection<Reading> readings) {
        for (Reading reading : readings) {
            publish(reading);
        }
    }

    @Override
    public void publish(Map<String, String> values) {
        final ConfigurationCommand configurations = new ConfigurationCommand(ConfigurationCommand.CommandType.SET, values);
        publish("configurations/current", configurations);
    }

    @Override
    public void publish(ActuatorStatus actuatorStatus) {
        publish("actuators/status", actuatorStatus);
    }
}
