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
import java.util.Map;

public class JsonSingleReferenceProtocol extends Protocol {

    public JsonSingleReferenceProtocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        super(client, actuatorHandler, configurationHandler);
    }

    @Override
    protected void subscribe() throws Exception {
        client.subscribe("actuators/commands/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final ActuatorCommand actuatorCommand = JsonUtil.deserialize(payload, ActuatorCommand.class);
                final String reference = topic.substring(("actuators/commands/" + client.getClientId() + "/").length());
                actuatorCommand.setReference(reference);
                if (actuatorCommand.getCommand() == ActuatorCommand.CommandType.SET) {
                    actuatorHandler.onActuationReceived(actuatorCommand);
                } else {
                    final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(actuatorCommand.getReference());
                    publish(actuatorStatus);
                }
            }
        });

        client.subscribe("configurations/commands/" + client.getClientId() + "/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), "UTF-8");
                final Map<String, Object> configuration = JsonUtil.deserialize(payload, Map.class);
                configurationHandler.onConfigurationReceived(configuration);
            }
        });
    }

    @Override
    public void publish(Reading reading) {
        publish("readings/" + client.getClientId() + "/" + reading.getRef(), reading);
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
        publish("configurations/current/" + client.getClientId(), configurations);
    }

    @Override
    public void publish(ActuatorStatus actuatorStatus) {
        publish("actuators/status/" + client.getClientId() + "/" + actuatorStatus.getReference(), actuatorStatus);
    }
}
