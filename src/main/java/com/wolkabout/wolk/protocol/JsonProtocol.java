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

import java.util.*;

public class JsonProtocol extends Protocol {

    private static final String P2D_ACTUATOR_SET = "p2d/actuator_set/d/";
    private static final String ACTUATOR_GET = "p2d/actuator_get/d/";

    private static final String CONFIGURATION_SET = "p2d/configuration_set/d/";
    private static final String CONFIGURATION_GET = "p2d/configuration_get/d/";
    private static final String SENSOR_READING = "d2p/sensor_reading/d/";

    public JsonProtocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        super(client, actuatorHandler, configurationHandler);
    }

    @Override
    protected void subscribe() throws Exception {
        client.subscribe(P2D_ACTUATOR_SET + client.getClientId() + "/r/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> actuation = JsonUtil.deserialize(message, HashMap.class);
                final Object value = actuation.get("value");

                final String reference = topic.substring((P2D_ACTUATOR_SET + client.getClientId() + "/r/").length());
                final ActuatorCommand actuatorCommand = new ActuatorCommand();
                actuatorCommand.setCommand(ActuatorCommand.CommandType.SET);
                actuatorCommand.setReference(reference);
                actuatorCommand.setValue(value.toString());
                actuatorHandler.onActuationReceived(actuatorCommand);
            }
        });

        client.subscribe(ACTUATOR_GET + client.getClientId() + "/r/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String reference = topic.substring((ACTUATOR_GET + client.getClientId() + "/r/").length());
                final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(reference);
                publish(actuatorStatus);
            }
        });

        client.subscribe(CONFIGURATION_SET + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> config = JsonUtil.deserialize(message, HashMap.class);
                configurationHandler.onConfigurationReceived(config);
            }
        });

        client.subscribe(CONFIGURATION_GET + client.getClientId(), new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final Map<String, String> configurations = configurationHandler.getConfigurations();
                publish(configurations);
            }
        });
    }

    @Override
    public void publish(Reading reading) {
        publish(SENSOR_READING + client.getClientId() + "/r/" + reading.getRef(), reading);
    }

    @Override
    public void publish(Collection<Reading> readings) {
        final HashMap<Long, Map<String, Object>> payloadByTime = new HashMap<>();
        for (Reading reading : readings) {
            if (payloadByTime.containsKey(reading.getUtc())) {
                final Map<String, Object> readingMap = payloadByTime.get(reading.getUtc());
                if (!readingMap.containsKey(reading.getRef())) {
                    readingMap.put(reading.getRef(), reading.getValue());
                }
            } else {
                final HashMap<String, Object> readingMap = new HashMap<>();
                readingMap.put("utc", reading.getUtc());
                readingMap.put(reading.getRef(), reading.getValue());
                payloadByTime.put(reading.getUtc(), readingMap);
            }
        }

        publish(SENSOR_READING + client.getClientId(), new ArrayList<>(payloadByTime.values()));
    }

    @Override
    public void publish(Map<String, String> values) {
        final ConfigurationCommand configurations = new ConfigurationCommand(ConfigurationCommand.CommandType.SET, values);
        publish(CONFIGURATION_GET + client.getClientId(), configurations);
    }

    @Override
    public void publish(ActuatorStatus actuatorStatus) {
        publish("d2p/actuator_status/d/" + client.getClientId() + "/r/" + actuatorStatus.getReference(), actuatorStatus);
    }
}
