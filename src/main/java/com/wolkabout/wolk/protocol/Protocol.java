package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.protocol.processor.ActuationProcessor;
import com.wolkabout.wolk.protocol.processor.ConfigurationProcessor;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.ConfigurationCommand;
import com.wolkabout.wolk.model.Reading;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);

    protected final MqttClient client;

    private ActuationProcessor actuationProcessor = new ActuationProcessor() {
        @Override
        public void onActuationReceived(ActuatorCommand actuatorCommand) {
            LOG.trace("Actuation received: " + actuatorCommand);
        }
    };

    private ConfigurationProcessor configurationProcessor = new ConfigurationProcessor() {
        @Override
        public void onConfigurationReceived(Map<String, Object> configuration) {
            LOG.trace("Configuration received: " + configuration);
        }
    };

    public Protocol(MqttClient client) {
        this.client = client;
        subscribe();
    }

    private void subscribe() {
        try {
            client.subscribe(getActuationTopic(), new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    final String payload = new String(message.getPayload(), "UTF-8");
                    final ActuatorCommand actuatorCommand = parseActuatorCommand(payload);
                    actuationProcessor.onActuationReceived(actuatorCommand);
                }
            });

            client.subscribe(getConfigurationTopic(), new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    final String payload = new String(message.getPayload(), "UTF-8");
                    final Map<String, Object> configuration = parseConfiguration(payload);
                    configurationProcessor.onConfigurationReceived(configuration);
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }

    public void setActuationProcessor(ActuationProcessor actuationProcessor) {
        this.actuationProcessor = actuationProcessor;
    }

    public void setConfigurationProcessor(ConfigurationProcessor configurationProcessor) {
        this.configurationProcessor = configurationProcessor;
    }

    protected abstract String getActuationTopic();
    protected abstract String getConfigurationTopic();

    protected abstract ActuatorCommand parseActuatorCommand(String payload);
    protected abstract Map<String, Object> parseConfiguration(String payload);

    public abstract void publish(Reading reading);
    public abstract void publish(List<Reading> readings);
    public abstract void publish(ConfigurationCommand configurations);
    public abstract void publish(ActuatorStatus actuatorStatus);
}
