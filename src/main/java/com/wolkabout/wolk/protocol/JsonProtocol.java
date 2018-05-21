package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.ConfigurationCommand;
import com.wolkabout.wolk.model.Reading;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonProtocol extends Protocol {

    public JsonProtocol(MqttClient client, ProtocolHandler handler) {
        super(client, handler);
    }

    @Override
    protected void subscribe() throws Exception {
        client.subscribe("p2d/actuator_set/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> actuation = JsonUtil.deserialize(message, HashMap.class);
                final Object value = actuation.get("value");

                final String reference = topic.substring(("p2d/actuator_set/" + client.getClientId() + "/r/").length());
                final ActuatorCommand actuatorCommand = new ActuatorCommand();
                actuatorCommand.setCommandType(ActuatorCommand.CommandType.SET);
                actuatorCommand.setReference(reference);
                actuatorCommand.setValue(value.toString());
                handler.onActuationReceived(actuatorCommand);
            }
        });

        client.subscribe("p2d/actuator_get/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String reference = topic.substring(("p2d/actuator_get/" + client.getClientId() + "/r/").length());
                final ActuatorStatus actuatorStatus = handler.getActuatorStatus(reference);
                publish(actuatorStatus);
            }
        });

        client.subscribe("p2d/configuration_set/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> config = JsonUtil.deserialize(message, HashMap.class);
                handler.onConfigurationReceived(config);
            }
        });

        client.subscribe("p2d/configuration_get/" + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final Map<String, String> configurations = handler.getConfigurations();
                publish(configurations);
            }
        });
    }

    @Override
    public void publish(Reading reading) {
        final Object payload = ""; // TODO
        publish( "d2p/sensor_reading/d/" + client.getClientId() + "/r/" + reading.getRef(), payload);
    }

    @Override
    public void publish(Collection<Reading> readings) {
        final Object payload = ""; // TODO
        publish( "d2p/sensor_reading/d/" + client.getClientId(), payload);
    }

    @Override
    public void publish(Map<String, String> values) {
        final ConfigurationCommand configurations = new ConfigurationCommand(ConfigurationCommand.CommandType.SET, values);
        publish("d2p/configuration_get/" + client.getClientId(), configurations);
    }

    @Override
    public void publish(ActuatorStatus actuatorStatus) {
        publish("d2p/actuator_status/" + client.getClientId() + "/r/" + actuatorStatus.getReference(), actuatorStatus);
    }
}
