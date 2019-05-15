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

public class JsonSingleReferenceProtocol extends Protocol {

    private static final String ACTUATOR_COMMANDS = "actuators/commands/";
    private static final String ACTUATOR_STATUS = "actuators/status/";

    private static final String CONFIGURATION_COMMANDS = "configurations/commands/";
    private static final String CONFIGURATION_SEND = "configurations/current/";

    private static final String SENSOR_READING = "readings/";
    private static final String EVENT = "events/";

    public JsonSingleReferenceProtocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        super(client, actuatorHandler, configurationHandler);
    }

    @Override
    public void subscribe() throws Exception {
        client.subscribe(ACTUATOR_COMMANDS + client.getClientId() + "/#", QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final ActuatorCommand actuatorCommand = JsonUtil.deserialize(payload, ActuatorCommand.class);
                final String reference = topic.substring((ACTUATOR_COMMANDS + client.getClientId() + "/").length());
                actuatorCommand.setReference(reference);
                if (actuatorCommand.getCommand() == ActuatorCommand.CommandType.SET) {
                    actuatorHandler.onActuationReceived(actuatorCommand);
                }

                final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(actuatorCommand.getReference());
                publishActuatorStatus(actuatorStatus);
            }
        });

        client.subscribe(CONFIGURATION_COMMANDS + client.getClientId(), QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final ConfigurationCommand configurationCommand = JsonUtil.deserialize(payload, ConfigurationCommand.class);

                if (configurationCommand.getType() == ConfigurationCommand.CommandType.SET) {
                    configurationHandler.onConfigurationReceived(configurationCommand.getValues());
                }

                final Map<String, Object> configuration = configurationHandler.getConfigurations();
                publishConfiguration(configuration);
            }
        });
    }

    @Override
    public void publishReading(Reading reading) {
        publish(SENSOR_READING + client.getClientId() + "/" + reading.getReference(), reading);
    }

    @Override
    public void publishReadings(Collection<Reading> readings) {
        if (readings.isEmpty()) {
            return;
        }

        final Map<String, List<Reading>> readingsByReference = new HashMap<>();
        for (Reading reading : readings) {
            if (!readingsByReference.containsKey(reading.getReference())) {
                readingsByReference.put(reading.getReference(), new ArrayList<Reading>());
            }

            readingsByReference.get(reading.getReference()).add(reading);
        }

        for (Map.Entry<String, List<Reading>> entry : readingsByReference.entrySet()) {
            publish(SENSOR_READING + client.getClientId() + "/" + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void publishAlarm(Alarm alarm) {
        publish(EVENT + client.getClientId() + "/" + alarm.getReference(), alarm);
    }

    @Override
    public void publishAlarms(Collection<Alarm> alarms) {
        if (alarms.isEmpty()) {
            return;
        }

        final Map<String, List<Alarm>> alarmsByReference = new HashMap<>();
        for (Alarm alarm : alarms) {
            if (!alarmsByReference.containsKey(alarm.getReference())) {
                alarmsByReference.put(alarm.getReference(), new ArrayList<Alarm>());
            }

            alarmsByReference.get(alarm.getReference()).add(alarm);
        }

        for (Map.Entry<String, List<Alarm>> entry : alarmsByReference.entrySet()) {
            publish(EVENT + client.getClientId() + "/" + entry.getKey(), entry.getValue());
        }
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
        publish(ACTUATOR_STATUS + client.getClientId() + "/" + actuatorStatus.getReference(), actuatorStatus);
    }
}
