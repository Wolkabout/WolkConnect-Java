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
import com.wolkabout.wolk.util.JsonMultivalueSerializer;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class JsonProtocol extends Protocol {

    private static final String ACTUATOR_SET = "p2d/actuator_set/d/";
    private static final String ACTUATOR_GET = "p2d/actuator_get/d/";
    private static final String ACTUATOR_STATUS = "d2p/actuator_status/d/";

    private static final String CONFIGURATION_SET = "p2d/configuration_set/d/";
    private static final String CONFIGURATION_GET = "p2d/configuration_get/d/";
    private static final String CONFIGURATION_SEND = "d2p/configuration_get/d/";

    private static final String SENSOR_READING = "d2p/sensor_reading/d/";
    private static final String EVENT = "d2p/events/d/";

    public JsonProtocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        super(client, actuatorHandler, configurationHandler);
    }

    @Override
    public void subscribe() throws Exception {
        client.subscribe(ACTUATOR_SET + client.getClientId() + "/r/#", QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> actuation = JsonUtil.deserialize(message, HashMap.class);
                final Object value = actuation.get("value");

                final String reference = topic.substring((ACTUATOR_SET + client.getClientId() + "/r/").length());
                final ActuatorCommand actuatorCommand = new ActuatorCommand();
                actuatorCommand.setCommand(ActuatorCommand.CommandType.SET);
                actuatorCommand.setReference(reference);
                actuatorCommand.setValue(value.toString());
                actuatorHandler.onActuationReceived(actuatorCommand);

                final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(reference);
                publishActuatorStatus(actuatorStatus);
            }
        });

        client.subscribe(ACTUATOR_GET + client.getClientId() + "/r/#", QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String reference = topic.substring((ACTUATOR_GET + client.getClientId() + "/r/").length());
                final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(reference);
                publishActuatorStatus(actuatorStatus);
            }
        });

        client.subscribe(CONFIGURATION_SET + client.getClientId(), QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final HashMap<String, Object> config = JsonUtil.deserialize(message, HashMap.class);
                configurationHandler.onConfigurationReceived(config);

                final Map<String, Object> configurations = configurationHandler.getConfigurations();
                publishConfiguration(configurations);
            }
        });

        client.subscribe(CONFIGURATION_GET + client.getClientId(), QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final Map<String, Object> configurations = configurationHandler.getConfigurations();
                publishConfiguration(configurations);
            }
        });
    }

    @Override
    public void publishReading(Reading reading) {
        publish(SENSOR_READING + client.getClientId() + "/r/" + reading.getReference(), reading);
    }

    @Override
    public void publishReadings(Collection<Reading> readings) {
        if (readings.isEmpty()) {
            return;
        }

        final HashMap<Long, Map<String, Object>> payloadByTime = new HashMap<>();
        for (Reading reading : readings) {
            if (payloadByTime.containsKey(reading.getUtc())) {
                final Map<String, Object> readingMap = payloadByTime.get(reading.getUtc());
                if (!readingMap.containsKey(reading.getReference())) {
                    readingMap.put(reading.getReference(), JsonMultivalueSerializer.valuesToString(reading.getValues()));
                }
            } else {
                final HashMap<String, Object> readingMap = new HashMap<>();
                readingMap.put("utc", reading.getUtc());
                readingMap.put(reading.getReference(), JsonMultivalueSerializer.valuesToString(reading.getValues()));
                payloadByTime.put(reading.getUtc(), readingMap);
            }
        }

        publish(SENSOR_READING + client.getClientId(), new ArrayList<>(payloadByTime.values()));
    }

    @Override
    public void publishAlarm(Alarm alarm) {
        publish(EVENT + client.getClientId() + "/r/" + alarm.getReference(), alarm);
    }

    @Override
    public void publishAlarms(Collection<Alarm> alarms) {
        if (alarms.isEmpty()) {
            return;
        }

        final HashMap<Long, Map<String, Object>> payloadByTime = new HashMap<>();
        for (Alarm alarm : alarms) {
            if (payloadByTime.containsKey(alarm.getUtc())) {
                final Map<String, Object> alarmMap = payloadByTime.get(alarm.getUtc());
                if (!alarmMap.containsKey(alarm.getReference())) {
                    alarmMap.put(alarm.getReference(), alarm.getValue());
                }
            } else {
                final HashMap<String, Object> alarmMap = new HashMap<>();
                alarmMap.put("utc", alarm.getUtc());
                alarmMap.put(alarm.getReference(), alarm.getValue());
                payloadByTime.put(alarm.getUtc(), alarmMap);
            }
        }

        publish(EVENT + client.getClientId(), new ArrayList<>(payloadByTime.values()));
    }

    @Override
    public void publishConfiguration(Map<String, Object> configuration) {
        final HashMap<String, Map<String, String>> payload = new HashMap<>();
        final HashMap<String, String> values = new HashMap<>();

        try {
            for (Map.Entry<String, Object> entry : configuration.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getClass().isArray()) {

                    List<String> multivalue = new ArrayList<String>();

                    int length = Array.getLength(entry.getValue());
                    for (int i = 0; i < length; ++i) {
                        multivalue.add(Objects.toString(Array.get(entry.getValue(), i), null));
                    }

                    values.put(entry.getKey(), JsonMultivalueSerializer.valuesToString(multivalue));
                } else if (entry.getValue() != null && entry.getValue() instanceof List) {

                    List<String> multivalue = ((List<Object>) entry.getValue()).stream()
                            .map(object -> Objects.toString(object, null))
                            .collect(Collectors.toList());

                    values.put(entry.getKey(), JsonMultivalueSerializer.valuesToString(multivalue));
                } else {
                    values.put(entry.getKey(), Objects.toString(entry.getValue(), null));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize configuration", e);
        }

        payload.put("values", values);

        publish(CONFIGURATION_SEND + client.getClientId(), payload);
    }

    @Override
    public void publishActuatorStatus(ActuatorStatus actuatorStatus) {
        publish(ACTUATOR_STATUS + client.getClientId() + "/r/" + actuatorStatus.getReference(), actuatorStatus);
    }
}
