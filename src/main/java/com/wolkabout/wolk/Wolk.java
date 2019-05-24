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

import com.wolkabout.wolk.firmwareupdate.FirmwareInstaller;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateProtocol;
import com.wolkabout.wolk.firmwareupdate.UrlFileDownloader;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareStatus;
import com.wolkabout.wolk.firmwareupdate.model.UpdateError;
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

    private static final int KEEP_ALIVE_INTERVAL_MIN = 10;

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Runnable publishTask = new Runnable() {
        @Override
        public void run() {
            publish();
        }
    };

    private ScheduledFuture<?> runningPublishTask;

    private final Runnable keepAliveTask = new Runnable() {
        @Override
        public void run() {
            protocol.publishPing();
        }
    };

    private ScheduledFuture<?> runningKeepAliveTask;

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
    private FirmwareUpdateProtocol firmwareUpdateProtocol;

    /**
     * Persistence mechanism for storing and retrieving data.
     */
    private Persistence persistence;

    /**
     * Flag for keep alive mechanism
     */
    private boolean keepAliveEnabled;

    public void connect() {
        try {
            client.connect(options);
        } catch (Exception e) {
            LOG.info("Could not connect to MQTT broker.", e);
            return;
        }

        subscribe();

        if (keepAliveEnabled) {
            startKeepAlive();
        }
    }

    /**
     * Disconnects from the MQTT broker.
     */
    public void disconnect() {
        try {
            client.publish(options.getWillDestination(), options.getWillMessage().getPayload(), 2, false);
            client.disconnect();
        } catch (MqttException e) {
            LOG.trace("Could not disconnect from MQTT broker.", e);
        }

        stopKeepAlive();
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
     * Manually publish stored readings.
     * Requires a persistence store.
     */
    public void publish() {
        if (persistence == null) {
            throw new IllegalStateException("Manual publishing requires persistence store.");
        }

        protocol.publishReadings(persistence.getAll());

        protocol.publishAlarms(persistence.getAllAlarms());
    }

    /**
     * Adds reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.

     * @param reference {@link Reading#reference}
     * @param value {@link Reading#values}
     */
    public void addReading(String reference, String value) {
        final Reading reading = new Reading(reference, value);
        addReading(reading);
    }

    /**
     * Adds multivalue reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference {@link Reading#reference}
     * @param values {@link Reading#values}
     */
    public void addReading(String reference, List<String> values) {
        final Reading reading = new Reading(reference, values);
        addReading(reading);
    }

    /**
     * Adds readings to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.

     * @param reading {@link Reading}
     */
    public void addReading(Reading reading) {
        if (persistence == null) {
            protocol.publishReading(reading);
        } else {
            persistence.addReading(reading);
        }
    }

    /**
     * Adds a reading collection to be published.
     * If the persistence store is set, readings will be stored. Otherwise, they will be published immediately.
     *
     * @param readings A collection of {@link Reading}
     */
    public void addReadings(Collection<Reading> readings) {
        if (persistence == null) {
            protocol.publishReadings(readings);
        } else {
            persistence.addReadings(readings);
        }
    }

    /**
     * Adds readings to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference {@link Alarm#reference}
     * @param value {@link Alarm#value}
     */
    public void addAlarm(String reference, boolean value) {
        final Alarm alarm = new Alarm(reference, value);
        if (persistence == null) {
            protocol.publishAlarm(alarm);
        } else {
            persistence.addAlarm(alarm);
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
     * @param version current firmware version.
     */
    public void publishFirmwareVersion(String version) {
        if (firmwareUpdateProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        firmwareUpdateProtocol.publishFirmwareVersion(version);
    }

    /**
     * Publishes the progress of firmware update.
     * To publish an error state use {link {@link #publishFirmwareUpdateStatus(UpdateError)}}
     * @param status Status of the firmware update.
     */
    public void publishFirmwareUpdateStatus(FirmwareStatus status) {
        if (firmwareUpdateProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        firmwareUpdateProtocol.publishFlowStatus(status);
    }

    /**
     * Publishes an error that occurred during firmware update.
     * @param error Error that terminated firmware update.
     */
    public void publishFirmwareUpdateStatus(UpdateError error) {
        if (firmwareUpdateProtocol == null) {
            throw new IllegalStateException("Firmware update protocol not configured.");
        }

        firmwareUpdateProtocol.publishFlowStatus(error);
    }

    private void subscribe() {
        try {
            protocol.subscribe();

            if (firmwareUpdateProtocol != null) {
                firmwareUpdateProtocol.subscribe();
            }
        } catch (Exception e) {
            LOG.debug("Unable to subscribe to all required topics.", e);
        }
    }

    private void startKeepAlive() {
        if (runningKeepAliveTask != null && !runningKeepAliveTask.isDone()) {
            return;
        }

        runningKeepAliveTask = executor.scheduleAtFixedRate(keepAliveTask, 0, KEEP_ALIVE_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    private void stopKeepAlive() {
        if (runningKeepAliveTask == null || runningKeepAliveTask.isDone()) {
            return;
        }

        runningKeepAliveTask.cancel(true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MqttBuilder mqttBuilder = new MqttBuilder(this);

        private ProtocolType protocolType = ProtocolType.JSON;

        private Collection<String> actuatorReferences;

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

        private boolean keepAliveEnabled = true;

        private Builder() {}

        public MqttBuilder mqtt() {
            return mqttBuilder;
        }

        public Builder protocol(ProtocolType protocolType) {
            if (protocolType == null) {
                throw new IllegalArgumentException("Protocol type cannot be null.");
            }

            if (firmwareUpdateEnabled && protocolType == ProtocolType.JSON) {
                throw new IllegalStateException("JSON protocol does not support firmware update.");
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

            if (protocolType == ProtocolType.JSON) {
                throw new IllegalStateException("JSON protocol does not support firmware update.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, UrlFileDownloader urlFileDownloader) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            if (protocolType == ProtocolType.JSON) {
                throw new IllegalStateException("JSON protocol does not support firmware update.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.urlFileDownloader = urlFileDownloader;
            return this;
        }

        public Builder disableKeepAlive() {
            keepAliveEnabled = false;
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

                        for (final String reference : actuatorReferences) {
                            wolk.publishActuatorStatus(reference);
                        }

                        wolk.publishConfiguration();
                    }

                    @Override
                    public void connectionLost(Throwable cause) {}

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {}

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                wolk.options = mqttBuilder.options();
                wolk.protocol = getProtocol(wolk.client);
                wolk.persistence = persistence;
                wolk.keepAliveEnabled = keepAliveEnabled;

                if (firmwareUpdateEnabled) {
                    wolk.firmwareUpdateProtocol = new FirmwareUpdateProtocol(wolk.client, firmwareInstaller, urlFileDownloader);
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
                case JSON_SINGLE_REFERENCE:
                    return new JsonSingleReferenceProtocol(client, actuatorHandler, configurationHandler);
                case JSON:
                    return new JsonProtocol(client, actuatorHandler, configurationHandler);
                default:
                    throw new IllegalArgumentException("Unknown protocol type: " + protocolType);
            }

        }
    }
}
