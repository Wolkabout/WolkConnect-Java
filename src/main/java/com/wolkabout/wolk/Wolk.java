/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fusesource.mqtt.client.*;
import org.fusesource.mqtt.client.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Handles the connection to the Wolkabout IoT Platform.
 */
public class Wolk {
    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";

    private static final String ACTUATORS_COMMANDS = "actuators/commands/";

    private static final int PUBLISH_DATA_ITEMS_COUNT = 50;

    private static final Logger LOG = LoggerFactory.getLogger(Wolk.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> receiveTask;

    private final CommandBuffer commandBuffer = new CommandBuffer();

    private final Device device;

    private final OutboundMessageFactory outboundMessageFactory;

    private Persistence persistence;

    private ActuationHandler actuationHandler;
    private ActuatorStatusProvider actuatorStatusProvider;

    private String host;
    private String caName;
    private FutureConnection futureConnection;

    private Wolk(final Device device) {
        this.device = device;

        switch (device.getProtocol()) {
            case JSON_SINGLE:
                outboundMessageFactory = new JsonSingleOutboundMessageFactory(device.getDeviceKey());
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + device.getProtocol());
        }
    }

    /**
     * Use this method to create MQTT connection. Use the subsequent builder methods to set up and establish the
     * connection.
     *
     * @param device describes the device we are connecting to the platform.
     * @return builder object.
     */
    public static WolkBuilder connectDevice(Device device) {
        if (device.getDeviceKey().isEmpty()) {
            throw new IllegalArgumentException("No device key present.");
        }
        return new WolkBuilder(device);
    }

    private void addToCommandBuffer(final CommandBuffer.Command command) {
        commandBuffer.add(command);
    }

    public void connect() {
        // 1. Establish connection
        LOG.debug("Connecting to " + host);
        futureConnection.connect().then(new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                LOG.info("Connected to " + host);

                // 2. Subscribe to actuator channels and start receiving
                subscribeAndReceive();

                // 3. Publish persisted readings...
                publish();
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Future connection failed to connect", throwable);
            }
        });
    }

    private void subscribeAndReceive() {
        for (String ref : device.getActuators()) {
            try {
                LOG.info("Subscribing to: " + ref);
                futureConnection.subscribe(new Topic[]{new Topic(ACTUATORS_COMMANDS + device.getDeviceKey() + "/" + ref, QoS.EXACTLY_ONCE)});
            } catch (Exception e) {
                LOG.error("Failed to subscribe to: " + ref, e);
            }

            publishActuatorStatus(ref);
        }

        if (receiveTask == null) {
            receiveTask = executorService.scheduleAtFixedRate((new Runnable() {
                @Override
                public void run() {
                    try {
                        receive(futureConnection);
                    } catch (InterruptedException interrupted) {
                        // Task was interrupted from disconnect directive.
                        LOG.info("Received disconnect signal");
                    } catch (Exception e) {
                        LOG.error("Error while trying to receive data", e);
                    }
                }
            }), 0, 5, TimeUnit.MILLISECONDS);
        }
    }

    private void publish(final String topic, final String payload) throws TimeoutException {
        LOG.debug("Publishing ==> " + topic + " : " + payload.substring(0, Math.min(300, payload.length())) + "...");
        try {
            futureConnection.publish(topic, payload.getBytes(), QoS.EXACTLY_ONCE, false).await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new TimeoutException("Publish timeout.");
        }
    }

    private void flushActuatorStatuses() {
        for (final String key : persistence.getActuatorStatusesKeys()) {
            try {
                final ActuatorStatus actuatorStatus = persistence.getActuatorStatus(key);
                final OutboundMessage message = outboundMessageFactory.makeFromActuatorStatuses(Collections.singletonList(actuatorStatus));

                LOG.info("Flushing " + message.getSerializedItemsCount() + " persisted actuator status(es)");
                publish(message.getTopic(), message.getPayload());
                persistence.removeActuatorStatus(key);
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutBound Message from ActuatorStatus(es)", e);
            } catch (TimeoutException e) {
                LOG.error("Publish timed out.");
            }
        }
    }

    private void flushAlarms() {
        for (final String key : persistence.getAlarmsKeys()) {
            try {
                final List<Alarm> alarms = persistence.getAlarms(key, PUBLISH_DATA_ITEMS_COUNT);
                final OutboundMessage message = outboundMessageFactory.makeFromAlarms(alarms);

                LOG.info("Flushing " + message.getSerializedItemsCount() + " persisted reading(s)");
                publish(message.getTopic(), message.getPayload());
                persistence.removeAlarms(key, message.getSerializedItemsCount());
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutputMessage from Alarm(s)");
            } catch (TimeoutException e) {
                LOG.error("Publish timed out.");
            }
        }
    }

    private void flushReadings() {
        for (final String key : persistence.getReadingsKeys()) {
            try {
                final List<Reading> readings = persistence.getReadings(key, 50);
                final OutboundMessage message = outboundMessageFactory.makeFromReadings(readings);

                LOG.info("Flushing " + message.getSerializedItemsCount() + " persisted reading(s)");
                publish(message.getTopic(), message.getPayload());
                persistence.removeReadings(key, message.getSerializedItemsCount());
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to build OutboundMessage from Reading(s)", e);
            } catch (TimeoutException e) {
                LOG.error("Publish timed out.");
            }
        }
    }

    private void flushPersistedData() {
        flushActuatorStatuses();

        flushAlarms();

        flushReadings();

        if (!persistence.isEmpty() && futureConnection.isConnected()) {
            LOG.debug("Scheduling sending of next persisted data batch");

            addToCommandBuffer(new CommandBuffer.Command() {
                @Override
                public void execute() {
                    flushPersistedData();
                }
            });
        }
    }

    private void receive(FutureConnection futureConnection) throws Exception {
        final Future<Message> receive = futureConnection.receive();
        final Message message = receive.await();

        final String payload = new String(message.getPayload());
        final String topic = message.getTopic();

        LOG.debug("Received command: " + payload);
        final ActuatorCommand command = new ObjectMapper().readValue(payload, ActuatorCommand.class);
        final String actuatorReference = topic.substring(topic.lastIndexOf("/") + 1);
        switch (command.getCommand()) {
            case SET:
                actuationHandler.handleActuation(actuatorReference, command.getValue());
            case STATUS:
                publishActuatorStatus(actuatorReference);
                break;
            default:
                LOG.warn("Unknown command. Ignoring.");
        }
        message.ack();
    }


    /**
     * Disconnect from the platform.
     */
    public void disconnect() {
        executorService.shutdownNow();
        futureConnection.disconnect();
        LOG.info("Disconnected from " + host);
    }

    /**
     * Add a single reading to buffer.
     * Can be called from multiple thread simultaneously.
     *
     * @param ref   of the sensor
     * @param value of the measurement
     */
    public void addReading(final String ref, final String value) {
        addReading(ref, value, System.currentTimeMillis() / 1000L);
    }

    /**
     * Add single reading to buffer.
     * Can be called from multiple thread simultaneously.
     *
     * @param ref   of the sensor
     * @param value of the measurement
     * @param time  timestamp
     */
    public void addReading(final String ref, final String value, final long time) {
        addToCommandBuffer(new CommandBuffer.Command() {
            @Override
            public void execute() {
                final Reading reading = new Reading(ref, value, time);

                LOG.info("Persisting " + reading);
                if (!persistence.putReading(reading.getReference(), reading)) {
                    LOG.error("Could not persist " + reading);
                }
            }
        });
    }

    public void addAlarm(final String ref, final String value) {
        addReading(ref, value, System.currentTimeMillis() / 1000);
    }

    public void addAlarm(final String ref, final String value, final long time) {
        addToCommandBuffer(new CommandBuffer.Command() {
            @Override
            public void execute() {
                final Alarm alarm = new Alarm(ref, value, time);

                LOG.info("Persisting " + alarm);
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
        addToCommandBuffer(new CommandBuffer.Command() {
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
     * @param actuatorReference reference of the actuator
     */
    public void publishActuatorStatus(final String actuatorReference) {
        addToCommandBuffer(new CommandBuffer.Command() {
            @Override
            public void execute() {
                final ActuatorStatus actuatorStatus = actuatorStatusProvider.getActuatorStatus(actuatorReference);
                persistence.putActuatorStatus(actuatorReference, new ActuatorStatus(actuatorStatus.getStatus(), actuatorStatus.getValue(), actuatorReference));
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
        }

        /**
         * Setup host url.
         *
         * @param host url
         *             <ul>
         *             <li>For SSL version use format "ssl://address:port". </li>
         *             <li>Otherwise use format "tcp://address:port". </li>
         *             </ul>
         * @return WolkBuilder.
         */
        public WolkBuilder toHost(String host) {
            instance.host = host;
            return this;
        }

        /**
         * Setup host url.
         *
         * @param host url
         *             <ul>
         *             <li>For SSL version use format "ssl://address:port". </li>
         *             <li>Otherwise use format "tcp://address:port". </li>
         *             </ul>
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
         * Establish connection to the platform.
         *
         * @return Wolk
         * @throws Exception if an error occurs while establishing the connection.
         */
        public Wolk connect() throws Exception {
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

            final MqttFactory mqttFactory = new MqttFactory()
                    .deviceKey(instance.device.getDeviceKey())
                    .password(instance.device.getPassword())
                    .host(instance.host);

            final MQTT client = instance.host.startsWith("ssl") ? mqttFactory.sslClient(instance.caName) : mqttFactory.noSslClient();
            instance.futureConnection = client.futureConnection();
            instance.connect();
            return instance;
        }

    }
}
