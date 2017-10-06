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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles the MQTT connection to the Wolkabout IoT Platform.
 */
public class Wolk {

    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";

    private static final String ACTUATORS_COMMANDS = "actuators/commands/";
    private static final String ACTUATORS_STATUS = "actuators/status/";

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ReadingsBuffer readingsBuffer;
    private ActuationHandler actuationHandler;
    private ActuatorStatusProvider actuatorStatusProvider;
    private Device device;
    private ScheduledFuture<?> publishTask;
    private String host = "";
    private FutureConnection futureConnection;
    private String caName;

    private static Logger LOG = LoggerFactory.getLogger(Wolk.class);

    private final Gson gson = new Gson();

    private Wolk(final Device device) {
        this.device = device;
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

    /**
     * Sets a time delta to be used when comparing reading times.
     * If two intervals are within the delta range, the lower time will be set for both readings.
     * Default is 0.
     *
     * @param delta Time interval in seconds.
     */
    public void setTimeDelta(final int delta) {
        readingsBuffer.setDelta(delta);
    }

    /**
     * Starts publishing readings on a given interval.
     *
     * @param interval Time interval in seconds to elapse between two publish attempts.
     */
    public void startAutoPublishing(final int interval) {
        publishTask = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                publish();
                if (!publishTask.isCancelled()) {
                    publishTask = executorService.schedule(this, interval, TimeUnit.SECONDS);
                }
            }
        }, interval, TimeUnit.SECONDS);
    }

    /**
     * Cancels a started automatic publishing task.
     */
    public void stopAutoPublishing() {
        if (publishTask != null) {
            publishTask.cancel(true);
        }
    }

    /**
     * Add a single reading to buffer.
     *
     * @param ref   of the sensor.
     * @param value of the measurement.
     */
    public void addReading(final String ref, final String value) {
        readingsBuffer.addReading(ref, value);
    }

    /**
     * Add single reading to buffer.
     *
     * @param time  timestamp.
     * @param ref   of the sensor
     * @param value of the measurement.
     */
    public void addReading(final long time, final String ref, final String value) {
        readingsBuffer.addReading(ref, value);
    }

    /**
     * Publishes all the data and clears the reading list if publishing was successful.
     */
    public void publish() {
        if (readingsBuffer.isEmpty()) {
            LOG.info("No new readings. Not publishing.");
            return;
        }

        try {
            final Protocol protocol = device.getProtocol();
            final String rootTopic = protocol.getReadingsTopic();
            if (protocol == Protocol.WOLK_SENSE) {
                LOG.debug("Publishing ==> " + rootTopic + device.getDeviceKey() + " : " + readingsBuffer.getFormattedData());
                futureConnection.publish(rootTopic + device.getDeviceKey(), readingsBuffer.getFormattedData().getBytes(), QoS.EXACTLY_ONCE, false).await();
            } else {
                for (String ref : readingsBuffer.getReferences()) {
                    LOG.debug("Publishing ==> " + rootTopic + device.getDeviceKey() + "/" + ref + " : " + readingsBuffer.getJsonFormattedData(ref));
                    futureConnection.publish(rootTopic + device.getDeviceKey() + "/" + ref, readingsBuffer.getJsonFormattedData(ref).getBytes(), QoS.EXACTLY_ONCE, false).await();
                }
            }
            readingsBuffer.removePublishedReadings();
            LOG.info("Publish successful. Readings buffer cleared.");
        } catch (Exception e) {
            LOG.error("Publishing data failed.", e);
        }
    }

    /**
     * Establish the MQTT connection with the platform.
     *
     * @throws Exception if the connection was not setup propely.
     */
    public void connect() throws Exception {
        connect(host.startsWith("ssl"));
    }

    /**
     * Establish the MQTT connection with the platform.
     *
     * @param sslEnabled true if you want ssl connection.
     * @throws Exception if the connection was not setup propely.
     */
    public void connect(boolean sslEnabled) throws Exception {

        final MqttFactory mqttFactory = new MqttFactory()
                .deviceKey(device.getDeviceKey())
                .password(device.getPassword())
                .host(this.host);
        futureConnection = (sslEnabled ? mqttFactory.sslClient(caName) : mqttFactory.noSslClient()).futureConnection();

        LOG.debug("Connecting to " + this.host);
        futureConnection.connect().await();
        LOG.info("Connected to " + this.host);
        for (String ref : device.getActuators()) {
            final Future<byte[]> qos = futureConnection.subscribe(new Topic[]{new Topic(ACTUATORS_COMMANDS + device.getDeviceKey() + "/" + ref, QoS.EXACTLY_ONCE)});
            LOG.info("Subscribed to : " + ref + " QoS: " + QoS.values()[qos.await()[0]]);
            publishActuatorStatus(ref);
        }

        executorService.scheduleAtFixedRate((new Runnable() {
            @Override
            public void run() {
                try {
                    receive(futureConnection);
                } catch (InterruptedException interrupted) {
                    // Task was interrupted from disconnect directive.
                    LOG.info("Received disconnect signal.");
                } catch (Exception e) {
                    LOG.error("Error while trying to receive data", e);
                }
            }
        }), 0, 50, TimeUnit.MILLISECONDS);
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
     * Send the status of the actuator specified by its reference. ActuatorStatusProvider is used to
     * retrieve the status of the actuator.
     *
     * @param actuatorReference reference of the actuator.
     * @throws Exception if there was an error while publishing.
     */
    public void publishActuatorStatus(String actuatorReference) throws Exception {
        final String topic = ACTUATORS_STATUS + device.getDeviceKey() + "/" + actuatorReference;
        final String payload = gson.toJson(actuatorStatusProvider.getActuatorStatus(actuatorReference));
        LOG.debug("Publishing status ==> " + topic + " : " + payload);
        futureConnection.publish(topic, payload.getBytes(), QoS.EXACTLY_ONCE, false).await();
    }

    private void receive(FutureConnection futureConnection) throws Exception {
        LOG.debug("Listening...");
        final Future<Message> receive = futureConnection.receive();
        final Message message = receive.await();
        final String payload = new String(message.getPayload());
        LOG.debug("Received " + payload);
        final String topic = message.getTopic();
        if (device.getProtocol() == Protocol.WOLK_SENSE) {
            final String actual = payload.substring(4, payload.length() - 2);
            final String[] actuation = actual.split(":");
            actuationHandler.handleActuation(actuation[0], actuation[1]);
        } else {
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
        }
        message.ack();
    }

    public static class WolkBuilder {

        private Wolk instance;

        private WolkBuilder(Device device) {
            instance = new Wolk(device);
            instance.readingsBuffer = new ReadingsBuffer(device.getProtocol());
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
         * Setup actuation handler.
         *
         * @param actuationHandler see  {@link ActuationHandler}
         * @return WolkBuilder.
         */
        public WolkBuilder actuationHandler(ActuationHandler actuationHandler) {
            instance.actuationHandler = actuationHandler;
            return this;
        }

        /**
         * Setup status provider for actuators.
         *
         * @param actuatorStatusProvider see {@link ActuatorStatus}
         * @return WolkBuilderk
         */
        public WolkBuilder actuatorStatusProvider(ActuatorStatusProvider actuatorStatusProvider) {
            instance.actuatorStatusProvider = actuatorStatusProvider;
            return this;
        }

        public WolkBuilder certificateAuthority(String ca) {
            instance.caName = ca;
            return this;
        }

        /**
         * Establish mqtt connection to the platform.
         *
         * @return Wolk.
         * @throws Exception when the connection could be established.
         */
        public Wolk connect() throws Exception {
            instance.connect();
            return instance;
        }

    }

}
