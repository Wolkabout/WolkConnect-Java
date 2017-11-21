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

import com.google.gson.Gson;
import org.fusesource.mqtt.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles the connection to the Wolkabout IoT Platform.
 */
public class Wolk {
    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";

    private static final String ACTUATORS_COMMANDS = "actuators/commands/";
    private static final String ACTUATORS_STATUS = "actuators/status/";

    private static final Logger LOG = LoggerFactory.getLogger(Wolk.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private final Queue<Reading> inMemoryReadings = new LinkedList<>();

    private final CommandBuffer commandBuffer = new CommandBuffer();

    private final Device device;

    private final ReadingSerializer readingSerializer;

    private PersistentReadingQueue persistentReadingQueue;
    private int persistedItemsPublishBatchSize;

    private ActuationHandler actuationHandler;
    private ActuatorStatusProvider actuatorStatusProvider;

    private String host;
    private String caName;
    private FutureConnection futureConnection;

    private final Gson gson = new Gson();

    private Wolk(final Device device) {
        this.device = device;

        switch (device.getProtocol()) {
            case JSON_SINGLE:
                readingSerializer = new JsonSingleReadingSerializer(device.getDeviceKey());
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
        try {
            // 1. Establish connection
            LOG.debug("Connecting to " + host);
            futureConnection.connect().await(5, TimeUnit.SECONDS);
            LOG.info("Connected to " + host);
            // 2. Subscribe to actuator channels and start receiving
            subscribeAndReceive();
            // 3. Publish persisted readings...
            publish();
        } catch (URISyntaxException e) {
            LOG.error("Invalid MQTT broker URI: " + host);
        } catch (TimeoutException e) {
            LOG.error("MQTT broker connect timeout");
        } catch (Exception e) {
            LOG.error("Exception ocurred while trying to connect.", e);
        }
    }

    private void subscribeAndReceive() throws Exception {
        for (String ref : device.getActuators()) {
            final Future<byte[]> qos = futureConnection.subscribe(new Topic[]{new Topic(ACTUATORS_COMMANDS + device.getDeviceKey() + "/" + ref, QoS.EXACTLY_ONCE)});
            LOG.info("Subscribed to: " + ref + ". QoS: " + QoS.values()[qos.await()[0]]);
            publishActuatorStatus(ref);
        }

        executorService.scheduleAtFixedRate((new Runnable() {
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

    private boolean publish(final String topic, final String payload) {
        try {
            LOG.debug("Publishing ==> " + topic + " : " + payload);
            futureConnection.publish(topic, payload.getBytes(), QoS.EXACTLY_ONCE, false).await(2, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void flushPersistedData() {
        if (persistentReadingQueue != null && !persistentReadingQueue.isEmpty()) {
            try {
                final List<Reading> readings = persistentReadingQueue.peekReadings(persistedItemsPublishBatchSize);
                final OutboundMessage message = readingSerializer.serialize(readings);

                LOG.info("Flushing " + message.getSerializedItemsCount() + " persisted reading(s)");
                if (publish(message.getTopic(), message.getPayload())) {
                    persistentReadingQueue.pollReadings(message.getSerializedItemsCount());
                } else {
                    LOG.warn("Flush failed");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to serialize reading(s)", e);
            }
        }

        if (!inMemoryReadings.isEmpty()) {
            try {
                final List<Reading> readings = new ArrayList<>(inMemoryReadings);
                final OutboundMessage message = readingSerializer.serialize(readings);

                LOG.info("Flushing " + message.getSerializedItemsCount() + " in-memory reading(s)");
                if (publish(message.getTopic(), message.getPayload())) {
                    for (long i = 0; i < message.getSerializedItemsCount(); ++i) {
                        inMemoryReadings.poll();
                    }
                } else {
                    LOG.warn("Flush failed");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Unable to serialize reading(s)", e);
            }
        }

        if (!inMemoryReadings.isEmpty() || (persistentReadingQueue != null && !persistentReadingQueue.isEmpty())) {
            LOG.info("Scheduling sending of next persisted data batch");

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
        final ActuatorCommand command = gson.fromJson(new String(message.getPayload()), ActuatorCommand.class);
        final String actuatorReference = topic.substring(topic.lastIndexOf("/") + 1);

        switch (command.getCommand()) {
            case SET:
                this.actuationHandler.handleActuation(actuatorReference, command.getValue());
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

                if (persistentReadingQueue != null) {
                    LOG.info("Persisting " + reading + " to persistent storage");
                    if (persistentReadingQueue.offer(reading)) {
                        return;
                    } else {
                        LOG.error("Could not persist " + reading + " to persistent store");
                    }
                }

                LOG.info("Persisting " + reading + " in-memory");
                inMemoryReadings.add(reading);
            }
        });
    }

    /**
     * Publishes all the data and clears persisted data if publishing was successful.
     * Can be called from multiple thread simultaneously.
     */
    public void publish() {
        if (futureConnection == null || !futureConnection.isConnected()) {
            LOG.info("Skipping publish, Reason: Not connected to WolkAbout IoT Platform");
            return;
        }

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
                final String topic = ACTUATORS_STATUS + device.getDeviceKey() + "/" + actuatorReference;
                final String payload = gson.toJson(actuatorStatusProvider.getActuatorStatus(actuatorReference));

                try {
                    LOG.debug("Publishing status ==> " + topic + " : " + payload);
                    futureConnection.publish(topic, payload.getBytes(), QoS.EXACTLY_ONCE, false).await(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("Could not publish actuator status with reference " + actuatorReference);
                }
            }
        });
    }

    public static class WolkBuilder {
        private final Wolk instance;

        private WolkBuilder(Device device) {
            instance = new Wolk(device);
            instance.host = Wolk.WOLK_DEMO_URL;
            instance.caName = Wolk.WOLK_DEMO_CA;

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
         * @param persistentReadingQueue         instance of {@link PersistentReadingQueue}
         * @param persistedItemsPublishBatchSize number of persisted items to publish at once, if number of persisted
         *                                       items is greater than this, persisted items will be published sequentially in batches of
         *                                       up to this many items depending on limitations of underlying protocol that is used
         * @return WolkBuilder
         */
        public WolkBuilder withPersistence(PersistentReadingQueue persistentReadingQueue, int persistedItemsPublishBatchSize) {
            instance.persistentReadingQueue = persistentReadingQueue;
            instance.persistedItemsPublishBatchSize = persistedItemsPublishBatchSize;
            return this;
        }

        /**
         * Establish connection to the platform.
         *
         * @return Wolk
         */
        public Wolk connect() throws Exception {
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
