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
package com.wolkabout.wolk;

import com.wolkabout.wolk.filemanagement.FirmwareInstaller;
import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.UrlFileDownloader;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.persistence.InMemoryPersistence;
import com.wolkabout.wolk.persistence.Persistence;
import com.wolkabout.wolk.protocol.*;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles the connection to the WolkAbout IoT Platform.
 */
public class Wolk {

    private static final Logger LOG = LoggerFactory.getLogger(Wolk.class);

    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";


    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Runnable publishTask = new Runnable() {
        @Override
        public void run() {
            publish();
        }
    };

    private final Runnable publishKeepAlive = new Runnable() {
        @Override
        public void run() {
            protocol.publishKeepAlive();
        }
    };
    private ScheduledFuture<?> runningPublishTask;
    private ScheduledFuture<?> runningPublishKeepAliveTask;


    /**
     * MQTT client.
     */
    private MqttClient client;

    /**
     * MQTT connect options
     */
    private MqttConnectOptions options;

    /**
     * Protocol for sending and receiving data.
     */
    private Protocol protocol;

    /**
     * Protocol for receiving firmware updates.
     */
    private FileManagementProtocol fileManagementProtocol;

    /**
     * Persistence mechanism for storing and retrieving data.
     */
    private Persistence persistence;


    public void connect() {
        try {
            client.connect(options);
        } catch (Exception e) {
            LOG.info("Could not connect to MQTT broker.", e);
            return;
        }

        subscribe();
        startPublishingKeepAlive(60);

    }

    /**
     * Disconnects from the MQTT broker.
     */
    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.publish(options.getWillDestination(), options.getWillMessage().getPayload(), 2, false);
                client.disconnect();
            }
            stopPublishingKeepAlive();
        } catch (MqttException e) {
            LOG.trace("Could not disconnect from MQTT broker.", e);
        }
    }

    /**
     * Start automatic reading publishing.
     * Readings are published every X seconds.
     * Automatic publishing requires a persistence store.
     *
     * @param seconds Time in seconds between 2 publishes.
     */
    public void startPublishing(int seconds) {
        if (persistence == null) {
            throw new IllegalStateException("Automatic publishing requires persistence store.");
        }

        if (runningPublishTask != null && !runningPublishTask.isDone()) {
            return;
        }

        runningPublishTask = executor.scheduleAtFixedRate(publishTask, 0, seconds, TimeUnit.SECONDS);
    }

    /**
     * Stop automatic reading publishing
     */
    public void stopPublishing() {
        if (runningPublishTask == null || runningPublishTask.isDone()) {
            return;
        }

        runningPublishTask.cancel(true);
    }

    /**
     * Start automatic reading publishing keep alive messages.
     * Messages are published every X seconds.
     *
     * @param seconds Time in seconds between 2 publishes.
     */
    public void startPublishingKeepAlive(int seconds) {
        if (runningPublishKeepAliveTask != null && !runningPublishKeepAliveTask.isDone()) {
            return;
        }

        runningPublishKeepAliveTask = executor.scheduleAtFixedRate(publishKeepAlive, 0, seconds, TimeUnit.SECONDS);
    }

    /**
     * Stop publishing keep alive messages.
     */
    public void stopPublishingKeepAlive() {
        if (runningPublishKeepAliveTask == null || runningPublishKeepAliveTask.isDone()) {
            return;
        }

        runningPublishKeepAliveTask.cancel(true);
    }

    /**
     * Manually publish stored readings.
     * Requires a persistence store.
     */
    public void publish() {
        if (persistence == null) {
            throw new IllegalStateException("Manual publishing requires persistence store.");
        }

        try {
            protocol.publishReadings(persistence.getAll());
        } catch (Exception e) {
            LOG.info("Could not publish readings", e);
        }

        try {
            protocol.publishAlarms(persistence.getAllAlarms());
        } catch (Exception e) {
            LOG.info("Could not publish alarms", e);
        }
    }

    /**
     * Adds reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference Reference of the sensor
     * @param value     Value obtained by the reading
     */
    public void addReading(String reference, String value) {
        final Reading reading = new Reading(reference, value);
        addReading(reading);
    }

    /**
     * Adds multivalue reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference Reference of the sensor
     * @param values    Values obtained by the reading
     */
    public void addReading(String reference, List<String> values) {
        final Reading reading = new Reading(reference, values);
        addReading(reading);
    }

    /**
     * Adds readings to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reading {@link Reading}
     */
    public void addReading(Reading reading) {
        if (persistence != null) {
            persistence.addReading(reading);
            return;
        }

        try {
            protocol.publishReading(reading);
        } catch (Exception e) {
            LOG.info("Could not publish reading: " + reading.getReference(), e);
        }
    }

    /**
     * Adds a reading collection to be published.
     * If the persistence store is set, readings will be stored. Otherwise, they will be published immediately.
     *
     * @param readings A collection of {@link Reading}
     */
    public void addReadings(Collection<Reading> readings) {
        if (persistence != null) {
            persistence.addReadings(readings);
            return;
        }

        try {
            protocol.publishReadings(readings);
        } catch (Exception e) {
            LOG.info("Could not publish readings", e);
        }
    }

    /**
     * Adds alarm to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     *
     * @param reference Reference of the alarm
     * @param active    Current state of the alarm
     */
    public void addAlarm(String reference, boolean active) {
        final Alarm alarm = new Alarm(reference, active);

        if (persistence != null) {
            persistence.addAlarm(alarm);
            return;
        }

        try {
            protocol.publishAlarm(alarm);
        } catch (Exception e) {
            LOG.info("Could not publish alarm: " + reference, e);
        }
    }

    /**
     * Publishes current configuration.
     */
    public void publishConfiguration() {
        try {
            protocol.publishCurrentConfig();
        } catch (Exception e) {
            LOG.info("Could not publish configuration", e);
        }
    }

    /**
     * Publishes current actuator status for the given reference.
     *
     * @param ref actuator reference.
     */
    public void publishActuatorStatus(String ref) {
        try {
            protocol.publishActuatorStatus(ref);
        } catch (Exception e) {
            LOG.info("Could not publish actuator status for actuator: " + ref, e);
        }
    }

    /**
     * Publishes the new version of the firmware.
     *
     * @param version current firmware version.
     */
    public void publishFirmwareVersion(String version) {
        if (fileManagementProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        try {
//            fileManagementProtocol.publishFirmwareVersion(version);
        } catch (Exception e) {
            LOG.info("Could not publish firmware version", e);
        }
    }

    /**
     * Publishes the progress of file transfer.
     * To publish an error state use {link {@link #publishFileTransferStatus(FileTransferError)}}
     *
     * @param status Status of the file transfer.
     */
    public void publishFileTransferStatus(FileTransferStatus status) {
        if (fileManagementProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        try {
//            fileManagementProtocol.publishFlowStatus(status);
        } catch (Exception e) {
            LOG.info("Could not publish firmware update status", e);
        }
    }

    /**
     * Publishes an error that occurred during firmware update.
     *
     * @param error Error that terminated firmware update.
     */
    public void publishFileTransferStatus(FileTransferError error) {
        if (fileManagementProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        try {
            fileManagementProtocol.publishFlowStatus(error);
        } catch (Exception e) {
            LOG.info("Could not publish firmware update status", e);
        }
    }

    private void subscribe() {
        try {
            protocol.subscribe();

            if (fileManagementProtocol != null) {
                fileManagementProtocol.subscribe();
            }
        } catch (Exception e) {
            LOG.debug("Unable to subscribe to all required topics.", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MqttBuilder mqttBuilder = new MqttBuilder(this);

        private ProtocolType protocolType = ProtocolType.WOLKABOUT_PROTOCOL;

        private Collection<String> actuatorReferences = new ArrayList<>();

        private ActuatorHandler actuatorHandler = new ActuatorHandler() {
            @Override
            public void onActuationReceived(ActuatorCommand actuatorCommand) {
                LOG.trace("Actuation received: " + actuatorCommand);
            }

            @Override
            public ActuatorStatus getActuatorStatus(String ref) {
                return null;
            }
        };

        private ConfigurationHandler configurationHandler = new ConfigurationHandler() {
            @Override
            public void onConfigurationReceived(Collection<Configuration> configuration) {
                LOG.trace("Configuration received: " + configuration);
            }

            @Override
            public Collection<Configuration> getConfigurations() {
                return new ArrayList<>();
            }
        };

        private Persistence persistence = new InMemoryPersistence();

        private boolean firmwareUpdateEnabled = false;

        private FirmwareInstaller firmwareInstaller = null;

        private UrlFileDownloader urlFileDownloader = new UrlFileDownloader();

        private Builder() {
        }

        public MqttBuilder mqtt() {
            return mqttBuilder;
        }

        public Builder protocol(ProtocolType protocolType) {
            if (protocolType == null) {
                throw new IllegalArgumentException("Protocol type cannot be null.");
            }

            this.protocolType = protocolType;
            return this;
        }

        public Builder actuator(Collection<String> actuatorReferences, ActuatorHandler actuatorHandler) {
            if (actuatorReferences.isEmpty()) {
                throw new IllegalArgumentException("Actuator references must be set.");
            }

            if (actuatorHandler == null) {
                throw new IllegalArgumentException("Actuator handler must be set.");
            }

            this.actuatorReferences = actuatorReferences;
            this.actuatorHandler = actuatorHandler;
            return this;
        }

        public Builder configuration(ConfigurationHandler configurationHandler) {
            if (configurationHandler == null) {
                throw new IllegalArgumentException("Configuration handler must be set.");
            }

            this.configurationHandler = configurationHandler;
            return this;
        }

        public Builder persistence(Persistence persistence) {
            if (persistence == null) {
                throw new IllegalArgumentException("Persistence must be set.");
            }

            this.persistence = persistence;
            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, UrlFileDownloader urlFileDownloader) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.urlFileDownloader = urlFileDownloader;
            return this;
        }

        public Wolk build() {

            try {
                final Wolk wolk = new Wolk();
                wolk.client = mqttBuilder.client();
                wolk.client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (reconnect) {
                            wolk.subscribe();
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });

                wolk.options = mqttBuilder.options();
                wolk.protocol = getProtocol(wolk.client);
                wolk.persistence = persistence;

                if (firmwareUpdateEnabled) {
                    wolk.fileManagementProtocol = new FileManagementProtocol(wolk.client, urlFileDownloader);
                    firmwareInstaller.setWolk(wolk);
                }

                actuatorHandler.setWolk(wolk);
                configurationHandler.setWolk(wolk);

                return wolk;
            } catch (MqttException mqttException) {
                throw new IllegalArgumentException("Unable to create MQTT connection.", mqttException);
            }
        }

        private Protocol getProtocol(MqttClient client) {
            switch (protocolType) {
                case WOLKABOUT_PROTOCOL:
                    return new WolkaboutProtocol(client, actuatorHandler, configurationHandler);
                default:
                    throw new IllegalArgumentException("Unknown protocol type: " + protocolType);
            }

        }
    }
}
