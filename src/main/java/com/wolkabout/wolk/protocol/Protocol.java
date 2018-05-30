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

import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Reading;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public abstract class Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);

    protected final MqttClient client;
    protected final ActuatorHandler actuatorHandler;
    protected final ConfigurationHandler configurationHandler;

    public Protocol(MqttClient client, ActuatorHandler actuatorHandler, ConfigurationHandler configurationHandler) {
        this.client = client;
        this.actuatorHandler = actuatorHandler;
        this.configurationHandler = configurationHandler;

        try {
            subscribe();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }

    protected abstract void subscribe() throws Exception;

    protected void publish(String topic, Object payload) {
        try {
            LOG.trace("Publishing to \'" + topic + "\' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), 1, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    public void publishCurrentConfig() {
        final Map<String, String> configurations = configurationHandler.getConfigurations();
        publish(configurations);
    }

    public void publishActuatorStatus(String ref) {
        final ActuatorStatus actuatorStatus = actuatorHandler.getActuatorStatus(ref);
        publish(actuatorStatus);
    }

    public abstract void publish(Reading reading);
    public abstract void publish(Collection<Reading> readings);
    public abstract void publish(Map<String, String> configurations);
    public abstract void publish(ActuatorStatus actuatorStatus);
}