package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Reading;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public abstract class Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);

    protected final MqttClient client;
    protected final ProtocolHandler handler;

    public Protocol(MqttClient client, ProtocolHandler handler) {
        this.client = client;
        this.handler = handler;

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

    public abstract void publish(Reading reading);
    public abstract void publish(Collection<Reading> readings);
    public abstract void publish(Map<String, String> configurations);
    public abstract void publish(ActuatorStatus actuatorStatus);
}
