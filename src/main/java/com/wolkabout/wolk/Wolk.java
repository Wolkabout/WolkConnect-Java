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

import com.wolkabout.wolk.firmwareupdate.CommandReceivedProcessor;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateProtocol;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareStatus;
import com.wolkabout.wolk.firmwareupdate.model.UpdateError;
import com.wolkabout.wolk.model.ActuatorCommand;
import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Alarm;
import com.wolkabout.wolk.model.Reading;
import com.wolkabout.wolk.persistence.InMemoryPersistence;
import com.wolkabout.wolk.persistence.Persistence;
import com.wolkabout.wolk.protocol.*;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Runnable publishTask = new Runnable() {
        @Override
        public void run() {
            publish();
        }
    };

    private ScheduledFuture<?> runningTask;

    /**
     * Connected MQTT client.
     */
    private MqttClient client;

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
     * Disconnects from the MQTT broker.
     */
    public void disconnect() {
        try {
            client.disconnect();
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

        runningTask = executor.scheduleAtFixedRate(publishTask, 0, seconds, TimeUnit.SECONDS);
    }

    /**
     * Stop automatic reading publishing
     */
    public void stopPublishing() {
        if (runningTask == null) {
            return;
        }

        runningTask.cancel(true);
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
        protocol.publishCurrentConfig();
    }

    /**
     * Publishes current actuator status for the given reference.
     * @param ref actuator reference.
     */
    public void publishActuatorStatus(String ref) {
        protocol.publishActuatorStatus(ref);
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MqttBuilder mqttBuilder = new MqttBuilder(this);

        private ProtocolType protocolType = ProtocolType.JSON;

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
            public void onConfigurationReceived(Map<String, Object> configuration) {
                LOG.trace("Configuration received: " + configuration);
            }

            @Override
            public Map<String, String> getConfigurations() {
                return new HashMap<>();
            }
        };

        private Persistence persistence = new InMemoryPersistence();

        private boolean firmwareUpdateEnabled = false;

        private CommandReceivedProcessor commandReceivedProcessor = null;

        private Builder() {}

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

        public Builder actuator(ActuatorHandler actuatorHandler) {
            if (actuatorHandler == null) {
                throw new IllegalStateException("Actuator handler must be set.");
            }

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
            this.persistence = persistence;
            return this;
        }

        public Builder enableFirmwareUpdate(CommandReceivedProcessor commandReceivedProcessor) {
            if (commandReceivedProcessor == null) {
                throw new IllegalArgumentException("CommandReceivedProcessor is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.commandReceivedProcessor = commandReceivedProcessor;
            return this;
        }

        public Wolk connect() {
            try {
                final Wolk wolk = new Wolk();
                wolk.client = mqttBuilder.connect();
                wolk.protocol = getProtocol(wolk.client);
                wolk.persistence = persistence;

                if (firmwareUpdateEnabled) {
                    wolk.firmwareUpdateProtocol = new FirmwareUpdateProtocol(wolk.client, commandReceivedProcessor);
                    commandReceivedProcessor.setWolk(wolk);
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
