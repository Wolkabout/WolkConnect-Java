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

import com.wolkabout.wolk.connectivity.ConnectivityService;
import com.wolkabout.wolk.connectivity.InboundMessageDeserializer;
import com.wolkabout.wolk.connectivity.OutboundMessageFactory;
import com.wolkabout.wolk.connectivity.model.InboundMessage;
import com.wolkabout.wolk.connectivity.model.OutboundMessage;
import com.wolkabout.wolk.connectivity.mqtt.MqttConnectivityService;
import com.wolkabout.wolk.connectivity.mqtt.MqttFactory;
import com.wolkabout.wolk.filetransfer.FileTransferPacket;
import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdate;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateCommand;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateStatus;
import org.fusesource.mqtt.client.MQTT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Handles the connection to the WolkAbout IoT Platform.
 */
public class Wolk implements ConnectivityService.Listener, FirmwareUpdate.Listener {
    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";

    private static final String ACTUATORS_COMMANDS = "actuators/commands/";

    private static final String SERVICE_FIRMWARE_COMMANDS = "service/commands/firmware/";
    private static final String SERVICE_BINARY_TRANSFER = "service/binary/";

    private static final int PUBLISH_DATA_ITEMS_COUNT = 50;

    private static final Logger LOG = LoggerFactory.getLogger(Wolk.class);

    private final CommandQueue commandQueue = new CommandQueue();

    private final Device device;

    private final OutboundMessageFactory outboundMessageFactory;
    private final InboundMessageDeserializer inboundMessageDeserializer;

    private Persistence persistence;

    private ActuationHandler actuationHandler;
    private ActuatorStatusProvider actuatorStatusProvider;

    private FirmwareUpdate firmwareUpdate;

    private String host;
    private String caName;

    private MqttConnectivityService connectivityService;

    private Wolk(Device device) {
        this.device = device;

        switch (device.getProtocol()) {
            case JSON_SINGLE:
                outboundMessageFactory = new JsonSingleOutboundMessageFactory(device.getDeviceKey());
                inboundMessageDeserializer = new JsonSingleInboundMessageDeserializer();
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + device.getProtocol());
        }
    }

    private void enqueueCommand(CommandQueue.Command command) {
        commandQueue.add(command);
    }

    public void connect() {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                connectivityService.connect();
            }
        });
    }

    private void reportFirmwareVersion() {
        LOG.debug("Publishing firmware version");
        final OutboundMessage firmwareVersionMessage = outboundMessageFactory.makeFromFirmwareVersion(firmwareUpdate.getFirmwareVersion());
        if (!connectivityService.publish(firmwareVersionMessage)) {
            LOG.error("Unable to publish firmware version");
        }
    }

    private void flushActuatorStatuses() {
        for (final String key : persistence.getActuatorStatusesKeys()) {
            try {
                final ActuatorStatus actuatorStatus = persistence.getActuatorStatus(key);
                final OutboundMessage outboundMessage =
                        outboundMessageFactory.makeFromActuatorStatuses(Collections.singletonList(actuatorStatus));

                LOG.info("Publishing {} persisted actuator status(es)", outboundMessage.getSerializedItemsCount());
                if (!connectivityService.publish(outboundMessage)) {
                    LOG.error("Failed to publish persisted actuator status(es)");
                    return;
                }

                persistence.removeActuatorStatus(key);
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutBound Message from ActuatorStatus(es)", e);
            }
        }
    }

    private void flushAlarms() {
        for (final String key : persistence.getAlarmsKeys()) {
            try {
                final List<Alarm> alarms = persistence.getAlarms(key, PUBLISH_DATA_ITEMS_COUNT);
                final OutboundMessage outboundMessage = outboundMessageFactory.makeFromAlarms(alarms);

                LOG.info("Publishing {} persisted alarm(s)", outboundMessage.getSerializedItemsCount());
                if (!connectivityService.publish(outboundMessage)) {
                    LOG.error("Failed to publish persisted alarm(s)");
                    return;
                }

                persistence.removeAlarms(key, outboundMessage.getSerializedItemsCount());
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutputMessage from Alarm(s)");
            }
        }
    }

    private void flushSensorReadings() {
        for (final String key : persistence.getSensorReadingsKeys()) {
            try {
                final List<SensorReading> readings = persistence.getSensorReadings(key, PUBLISH_DATA_ITEMS_COUNT);
                final OutboundMessage outboundMessage = outboundMessageFactory.makeFromReadings(readings);

                LOG.info("Publishing {} persisted sensor reading(s)", outboundMessage.getSerializedItemsCount());
                if (!connectivityService.publish(outboundMessage)) {
                    LOG.error("Failed to publish persisted sensor reading(s)");
                    return;
                }
                persistence.removeSensorReadings(key, outboundMessage.getSerializedItemsCount());
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutboundMessage from Reading(s)", e);
            }
        }
    }

    private void flushPersistedData() {
        flushActuatorStatuses();

        flushAlarms();

        flushSensorReadings();

        if (!persistence.isEmpty() && connectivityService.isConnected()) {
            LOG.debug("Scheduling sending of next persisted data batch");

            enqueueCommand(new CommandQueue.Command() {
                @Override
                public void execute() {
                    flushPersistedData();
                }
            });
        }
    }

    private void handleActuatorCommand(ActuatorCommand command) {
        LOG.info("Handling actuator command: {}", command.getType());
        switch (command.getType()) {
            case SET:
                actuationHandler.handleActuation(command.getReference(), command.getValue());
            case STATUS:
                publishActuatorStatus(command.getReference());
                break;
            default:
                LOG.warn("Unknown command. Ignoring.");
        }
    }

    private void handleFileTransferPacket(FileTransferPacket packet) {
        firmwareUpdate.handlePacket(packet);
    }

    private void handleFirmwareUpdateCommand(FirmwareUpdateCommand command) {
        firmwareUpdate.handleCommand(command);
    }

    private void subscribeToActuatorCommands() {
        for (String ref : device.getActuators()) {
            try {
                LOG.debug("Subscribing to actuator commands. Reference: {}", ref);
                connectivityService.subscribe(ACTUATORS_COMMANDS + device.getDeviceKey() + "/" + ref);
            } catch (Exception e) {
                LOG.error("Failed to subscribe to: {}", ref, e);
            }
        }
    }

    private void subscribeToFirmwareUpdate() {
        LOG.info("Subscribing to firmware update commands");
        connectivityService.subscribe(SERVICE_FIRMWARE_COMMANDS + device.getDeviceKey());
        connectivityService.subscribe(SERVICE_BINARY_TRANSFER + device.getDeviceKey());
    }

    @Override
    public void onConnected() {
        subscribeToActuatorCommands();

        subscribeToFirmwareUpdate();

        reportFirmwareVersion();
        firmwareUpdate.reportFirmwareUpdateResult();

        publishCurrentActuatorStatuses();

        // 'publish' is called here to send persisted sensor readings, alarms, and actuator statuses
        publish();
    }

    private void publishCurrentActuatorStatuses() {
        for (final String reference : device.getActuators()) {
            publishActuatorStatus(reference);
        }
    }

    @Override
    public void onInboundMessage(InboundMessage inboundMessage) {
        final String topic = inboundMessage.getChannel();

        if (topic.startsWith(ACTUATORS_COMMANDS)) {
            LOG.info("Received actuator command");

            final ActuatorCommand command = inboundMessageDeserializer.deserializeActuatorCommand(inboundMessage);
            handleActuatorCommand(command);
        } else if (topic.startsWith(SERVICE_FIRMWARE_COMMANDS)) {
            LOG.info("Received firmware update command");

            final FirmwareUpdateCommand firmwareUpdateCommand = inboundMessageDeserializer.deserializeFirmwareUpdateCommand(inboundMessage);
            handleFirmwareUpdateCommand(firmwareUpdateCommand);
        } else if (topic.startsWith(SERVICE_BINARY_TRANSFER)) {
            LOG.info("Received file transfer packet");

            final FileTransferPacket packet = new FileTransferPacket(inboundMessage.getBinaryPayload());
            handleFileTransferPacket(packet);
        } else {
            LOG.warn("Inbound message received on '{}' is ignored", topic);
        }
    }

    @Override
    public void onStatus(FirmwareUpdateStatus status) {
        final OutboundMessage outboundMessage = outboundMessageFactory.makeFromFirmwareUpdateStatus(status);
        connectivityService.publish(outboundMessage);
    }

    @Override
    public void onFilePacketRequest(FileTransferPacketRequest request) {
        final OutboundMessage outboundMessage = outboundMessageFactory.makeFromFileTransferPacketRequest(request);
        connectivityService.publish(outboundMessage);
    }

    /**
     * Use this method to create MQTT connection. Use the subsequent builder methods to set up and establish the
     * connection.
     *
     * @param device Describes the device we are connecting to the platform
     * @return WolkBuilder
     */
    public static WolkBuilder connectDevice(Device device) {
        if (device.getDeviceKey().isEmpty()) {
            throw new IllegalArgumentException("No device key present.");
        }

        return new WolkBuilder(device);
    }

    /**
     * Disconnect from the platform.
     */
    public void disconnect() {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                connectivityService.disconnect();
            }
        });
    }

    /**
     * Add a single sensor reading.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of the sensor
     * @param value Value of the measurement
     */
    public void addSensorReading(String ref, String value) {
        addSensorReading(ref, value, System.currentTimeMillis() / 1000L);
    }

    /**
     * Add single sensor reading.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of the sensor
     * @param value Value of the measurement
     * @param time  UTC timestamp, in seconds or milliseconds
     */
    public void addSensorReading(final String ref, final String value, final long time) {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                final SensorReading reading = new SensorReading(ref, value, time);

                LOG.debug("Persisting {}", reading);
                if (!persistence.putSensorReading(reading.getReference(), reading)) {
                    LOG.error("Could not persist {}", reading);
                }
            }
        });
    }

    /**
     * Add a single sensor reading.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of the sensor
     * @param value Value of the measurement
     * @deprecated Use {@link #addSensorReading(String, String)} instead
     */
    @Deprecated
    public void addReading(String ref, String value) {
        addReading(ref, value, System.currentTimeMillis() / 1000L);
    }

    /**
     * Add single sensor reading.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of the sensor
     * @param value Value of the measurement
     * @param time  UTC timestamp, in seconds or milliseconds
     * @deprecated Use {@link #addSensorReading(String, String, long)} instead
     */
    @Deprecated
    public void addReading(final String ref, final String value, final long time) {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                final SensorReading reading = new SensorReading(ref, value, time);

                LOG.debug("Persisting {}", reading);
                if (!persistence.putSensorReading(reading.getReference(), reading)) {
                    LOG.error("Could not persist {}", reading);
                }
            }
        });
    }

    /**
     * Add single alarm.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of he alarm
     * @param value Value of the measurement
     */
    public void addAlarm(String ref, String value) {
        addAlarm(ref, value, System.currentTimeMillis() / 1000);
    }

    /**
     * Add single alarm.
     * Can be called from multiple threads simultaneously.
     *
     * @param ref   Reference of he alarm
     * @param value Value of the measurement
     * @param time  UTC timestamp, in seconds or milliseconds
     */
    public void addAlarm(final String ref, final String value, final long time) {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                final Alarm alarm = new Alarm(ref, value, time);

                LOG.debug("Persisting {}", alarm);
                if (!persistence.putAlarm(alarm.getReference(), alarm)) {
                    LOG.error("Could not persist " + alarm);
                }
            }
        });
    }

    /**
     * Publishes all the data and clears persisted data if publishing was successful.
     * Can be called from multiple thread simultaneously.
     */
    public void publish() {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                flushPersistedData();
            }
        });
    }

    /**
     * Send the status of the actuator specified by its reference. ActuatorStatusProvider is used to
     * retrieve the status of the actuator.
     * <p>
     * Can be called from multiple thread simultaneously.
     *
     * @param reference Reference of the actuator
     */
    public void publishActuatorStatus(final String reference) {
        enqueueCommand(new CommandQueue.Command() {
            @Override
            public void execute() {
                LOG.debug("Obtaining actuator status for reference {}", reference);
                final ActuatorStatus actuatorStatus = actuatorStatusProvider.getActuatorStatus(reference);
                persistence.putActuatorStatus(reference, new ActuatorStatus(actuatorStatus.getStatus(), actuatorStatus.getValue(), reference));
                flushActuatorStatuses();
            }
        });
    }

    public static class WolkBuilder {
        private final Wolk instance;

        private WolkBuilder(Device device) {
            instance = new Wolk(device);
            instance.host = Wolk.WOLK_DEMO_URL;
            instance.caName = Wolk.WOLK_DEMO_CA;

            instance.persistence = new InMemoryPersistence();

            instance.firmwareUpdate = new FirmwareUpdate("", null, 0, null, null);
            instance.firmwareUpdate.setListener(instance);
        }

        /**
         * Setup host URI.
         * If port is not provided it will be inferred from URI scheme.
         *
         * @param host URI to WolkAbout IoT platform instance
         *             <ul>
         *             <li>For SSL version use format "ssl://address:port". </li>
         *             <li>Otherwise use format "tcp://address:port". </li>
         *             </ul>
         * @return WolkBuilder.
         */
        public WolkBuilder toHost(String host) {
            // When port is not present, splitting by ':' yields array of 2 elements
            if (host.split(":").length == 2) {
                host = host + ":" + (host.toLowerCase().startsWith("ssl") ? 8883 : 1883);
            }

            instance.host = host;
            return this;
        }

        /**
         * Setup host URI.
         *
         * @param host URI to WolkAbout IoT platform instance
         * @return WolkBuilder
         */
        public WolkBuilder toHost(URI host) {
            instance.host = host.toString();
            return this;
        }

        /**
         * Setup actuation handler.
         *
         * @param actuationHandler see  {@link ActuationHandler}
         * @return WolkBuilder
         */
        public WolkBuilder actuationHandler(ActuationHandler actuationHandler) {
            instance.actuationHandler = actuationHandler;
            return this;
        }

        /**
         * Setup status provider for actuators.
         *
         * @param actuatorStatusProvider see {@link ActuatorStatus}
         * @return WolkBuilder
         */
        public WolkBuilder actuatorStatusProvider(ActuatorStatusProvider actuatorStatusProvider) {
            instance.actuatorStatusProvider = actuatorStatusProvider;
            return this;
        }

        /**
         * Setup Certificate Authority.
         *
         * @param ca Path to CA file
         * @return WolkBuilder
         */
        public WolkBuilder certificateAuthority(String ca) {
            instance.caName = ca;
            return this;
        }

        /**
         * Setup with persistence (aka Offline buffering).
         *
         * @param persistence instance of {@link Persistence}
         * @return WolkBuilder
         */
        public WolkBuilder withPersistence(Persistence persistence) {
            instance.persistence = persistence;
            return this;
        }

        /**
         * Setup firmware update (aka OTA) with firmware download via WolkAbout IoT platform.
         *
         * @param firmwareVersion         current firmware version
         * @param firmwareUpdateHandler   instance of {@link FirmwareUpdateHandler}
         * @param downloadDirectory       directory to which firmware file will be downloaded
         * @param maximumFirmwareSize     maximum size of firmware file, in bytes
         * @param firmwareDownloadHandler handler for downloading firmware file via URL. {@code null} if not used
         * @return WolkBuilder
         * @throws IllegalArgumentException if {@code downloadDirectory} does not point to directory, or directory does not exist
         */
        public WolkBuilder withFirmwareUpdate(String firmwareVersion, FirmwareUpdateHandler firmwareUpdateHandler, Path downloadDirectory,
                                              long maximumFirmwareSize, FirmwareDownloadHandler firmwareDownloadHandler) throws IllegalArgumentException {
            if (!downloadDirectory.toFile().isDirectory() || !downloadDirectory.toFile().exists()) {
                throw new IllegalArgumentException("Given path does not point to directory.");
            }

            instance.firmwareUpdate = new FirmwareUpdate(firmwareVersion, downloadDirectory, maximumFirmwareSize, firmwareUpdateHandler, firmwareDownloadHandler);
            instance.firmwareUpdate.setListener(instance);
            return this;
        }

        /**
         * Establish connection to the platform.
         *
         * @return Built {@link Wolk}
         * @throws Exception if building {@link Wolk} fails, or an error occurs while establishing the connection
         */
        public Wolk connect() throws Exception {
            validateActuationCallbacks();

            buildConnectivity();

            instance.connect();
            return instance;
        }

        private void validateActuationCallbacks() throws IllegalStateException {
            if (instance.device.getActuators().length != 0) {
                if (instance.actuatorStatusProvider == null || instance.actuationHandler == null) {
                    throw new IllegalStateException("Device has actuator references, ActuatorStatusProvider and ActuationHandler must be set.");
                }
            } else {
                // Provide default implementation to avoid ugliness of 'if ( ... != null )'
                actuationHandler(new ActuationHandler() {
                    @Override
                    public void handleActuation(String reference, String value) {
                    }
                });

                actuatorStatusProvider(new ActuatorStatusProvider() {
                    @Override
                    public ActuatorStatus getActuatorStatus(String ref) {
                        return new ActuatorStatus(ActuatorStatus.Status.ERROR, "");
                    }
                });
            }
        }

        private void buildConnectivity() throws Exception {
            final MqttFactory mqttFactory = new MqttFactory()
                    .deviceKey(instance.device.getDeviceKey())
                    .password(instance.device.getPassword())
                    .cleanSession(true)
                    .host(instance.host);

            final MQTT client = instance.host.startsWith("ssl") ? mqttFactory.sslClient(instance.caName) : mqttFactory.noSslClient();
            instance.connectivityService = new MqttConnectivityService(client);
            instance.connectivityService.setListener(instance);
        }
    }
}
