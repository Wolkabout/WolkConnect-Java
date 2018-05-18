package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.protocol.Protocol;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FirmwareUpdateProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);

    private final static IMqttMessageListener LOGGING_LISTENER = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            LOG.trace("Topic: " + topic + "\n Message: " + message);
        }
    };

    protected MqttClient client;

    private IMqttMessageListener onFirmwareFileInfoReceived;
    private IMqttMessageListener onFirmwareUrlReceived;
    private IMqttMessageListener onFirmwareInstallCommandReceived;
    private IMqttMessageListener onFirmwareAbortCommandReceived;

    public FirmwareUpdateProtocol(MqttClient client) {
        this.client = client;
    }

    protected abstract String getFileInfoTopic();
    protected abstract String getUriTopic();
    protected abstract String getInstallCommandTopic();
    protected abstract String getAbortCommandTopic();

    public void setOnFirmwareFileInfoReceived(IMqttMessageListener onFirmwareFileInfoReceived) {
        this.onFirmwareFileInfoReceived = onFirmwareFileInfoReceived;
    }

    public void setOnFirmwareUrlReceived(IMqttMessageListener onFirmwareUrlReceived) {
        this.onFirmwareUrlReceived = onFirmwareUrlReceived;
    }

    public void setOnFirmwareInstallCommandReceived(IMqttMessageListener onFirmwareInstallCommandReceived) {
        this.onFirmwareInstallCommandReceived = onFirmwareInstallCommandReceived;
    }

    public void setOnFirmwareAbortCommandReceived(IMqttMessageListener onFirmwareAbortCommandReceived) {
        this.onFirmwareAbortCommandReceived = onFirmwareAbortCommandReceived;
    }

    public void initialize() {
        try {
            client.subscribe(getFileInfoTopic(), onFirmwareFileInfoReceived);
            client.subscribe(getUriTopic(), onFirmwareUrlReceived);
            client.subscribe(getInstallCommandTopic(), onFirmwareInstallCommandReceived);
            client.subscribe(getAbortCommandTopic(), onFirmwareAbortCommandReceived);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }
}
