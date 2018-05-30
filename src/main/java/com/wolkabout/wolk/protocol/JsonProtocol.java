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
package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JsonProtocol extends Protocol {

    public JsonProtocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        super(client, actuatorHandler, configurationHandler);
    }

    @Override
    protected void subscribe() throws Exception {
        client.subscribe("p2d/actuator_set/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> actuation = JsonUtil.deserialize(message, HashMap.class);
                final Object value = actuation.get("value");

                final String reference = topic.substring(("p2d/actuator_set/" + client.getClientId() + "/r/").length());
                final ActuatorCommand actuatorCommand = new ActuatorCommand();
                actuatorCommand.setCommand(ActuatorCommand.CommandType.SET);
                actuatorCommand.setReference(reference);
                actuatorCommand.setValue(value.toString());
                actuatorHandler.onActuationReceived(actuatorCommand);
            }
        });

        client.subscribe("p2d/actuator_get/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String reference = topic.substring(("p2d/actuator_get/" + client.getClientId() + "/r/").length());
                final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(reference);
                publish(actuatorStatus);
            }
        });

        client.subscribe("p2d/configuration_set/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> config = JsonUtil.deserialize(message, HashMap.class);
                configurationHandler.onConfigurationReceived(config);
            }
        });

        client.subscribe("p2d/configuration_get/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final Map<String, String> configurations = configurationHandler.getConfigurations();
                publish(configurations);
            }
        });
    }

    @Override
    public void publish(Reading reading) {
        publish( "d2p/sensor_reading/d/" + client.getClientId() + "/r/" + reading.getRef(), reading);
    }

    @Override
    public void publish(Collection<Reading> readings) {
        final HashMap<Long, Map<String, String>> payloadByTime = new HashMap<>();
        for (Reading reading : readings) {
            if (payloadByTime.containsKey(reading.getUtc())) {
                final Map<String, String> readingMap = payloadByTime.get(reading.getUtc());
                if (!readingMap.containsKey(reading.getRef())) {
                    readingMap.put(reading.getRef(), reading.getValue());
                }
            } else {
                final HashMap<String, String> readingMap = new HashMap<>();
                readingMap.put(reading.getRef(), reading.getValue());
                payloadByTime.put(reading.getUtc(), readingMap);
            }
        }

        for (Map.Entry<Long, Map<String, String>> entry : payloadByTime.entrySet()) {
            final HashMap<String, Object> payload = new HashMap<>();

            payload.put("utc", entry.getKey());
            for (Map.Entry<String, String> groupedReading : entry.getValue().entrySet()) {
                payload.put(groupedReading.getKey(), groupedReading.getValue());
            }

            publish( "d2p/sensor_reading/d/" + client.getClientId(), payload);
        }
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
