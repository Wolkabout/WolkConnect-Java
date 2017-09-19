/*
 * Copyright 2016 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wolkabout.wolk;

import com.google.gson.Gson;
import org.fusesource.mqtt.client.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Wolk {

    private static final MQTT mqtt = new MQTT();
    public static final String ACTUATORS_COMMANDS = "actuators/commands/";
    public static final String ACTUATORS_STATUS = "actuators/commands/";

    private final PublishingService publishingService;
    private final ReadingsBuffer readingsBuffer = new ReadingsBuffer();
    private ActuationHandler actuationHandler;

    public void setActuatorStatusProvider(ActuatorStatusProvider actuatorStatusProvider) {
        this.actuatorStatusProvider = actuatorStatusProvider;
    }

    private ActuatorStatusProvider actuatorStatusProvider;

    private final Device device;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);
    private ScheduledFuture<?> publishTask;

    private FutureConnection futureConnection;
    private Logger logger = new Logger() {
    };

    public interface ActuatorStatusProvider {
        ActuatorStatus getActuatorStatus(String ref);
    }

    public Wolk(final Device device) {
        publishingService = new PublishingService(device);
        this.device = device;
    }

    public Wolk(final Device device, final String host) {
        publishingService = new PublishingService(device, host);
        this.device = device;
    }

    /**
     * Sets the logging mechanism for the library.
     *
     * @param logger Platform specific implementation of the logging mechanism.
     */
    public void setLogger(final Logger logger) {
        this.logger = logger;
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
     * Adds a reading of the given type for the current time.
     *
     * @param type  Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final ReadingType type, final String value) {
        readingsBuffer.addReading(type, value);
    }

    public void addReading(final String ref, final String value) {
        readingsBuffer.addReading(ref, value);
    }

    /**
     * Adds a reading of the given type for the given time.
     *
     * @param time  Time of the reading.
     * @param type  Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final long time, final ReadingType type, final String value) {
        readingsBuffer.addReading(time, type, value);
    }

    public void addReading(final long time, final String ref, final String value) {
        readingsBuffer.addReading(ref, value);
    }


    /**
     * Publishes all the data and clears the reading list if publishing was successful.
     */
    public void publish() {
        final ActuationHandler actuationHandler = (reference, value) -> {
            logger.info("Actuation on " + reference + " value " + value);
        };
        if (readingsBuffer.isEmpty()) {
            logger.info("No new readings. Not publishing.");
            return;
        }

        try {
            futureConnection.publish("sensors/" + device.getSerialId(), readingsBuffer.getFormattedData().getBytes(), QoS.AT_LEAST_ONCE, false);
            for (String ref : readingsBuffer.getReferences()) {
                futureConnection.publish("readings/" + device.getSerialId() + "/" + ref, readingsBuffer.getJsonFormattedData(ref).getBytes(), QoS.AT_LEAST_ONCE, false);
            }
            readingsBuffer.removePublishedReadings();
            logger.info("Publish successful. Readings list trimmed.");
        } catch (Exception e) {
            logger.error("Publishing data failed.", e);
        }
    }

    /*
        Connect to the
     */
    public void connect(ActuationHandler actuationHandler) throws Exception {
        initMqtt();
        this.actuationHandler = actuationHandler;
        futureConnection = mqtt.futureConnection();
        futureConnection.connect();


        for (String ref : device.getActuators()) {
            futureConnection.subscribe(new Topic[]{new Topic(ACTUATORS_COMMANDS + device.getSerialId() + "/" + ref, QoS.EXACTLY_ONCE)});
        }
        executorService.scheduleAtFixedRate(() -> {
            try {
                receive(futureConnection);
            } catch (InterruptedException interrupted) {
                System.out.println("Shutdown");
                // Normal...
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public void disconnect() {
        executorService.shutdownNow();
        futureConnection.disconnect();
    }

    private void receive(FutureConnection futureConnection) throws Exception {
        final Future<Message> receive = futureConnection.receive();
        final Message message = receive.await();
        final String payload = new String(message.getPayload());
        if (payload.startsWith("SET")) {
            String actual = payload.substring(4, payload.length() - 2);
            final String[] actuation = actual.split(":");
            actuationHandler.handleActuation(actuation[0], actuation[1]);
        } else {
            Gson gson = new Gson();
            final ActuatorCommand command = gson.fromJson(new String(message.getPayload()), ActuatorCommand.class);

            switch (command.getCommand()) {
                case SET:
                    this.actuationHandler.handleActuation(message.getTopic(), command.getValue());
                    break;
                case STATUS:
                    String topic = message.getTopic().replace("commands", "status");
                    futureConnection.publish(topic, gson.toJson(actuatorStatusProvider.getActuatorStatus(topic)).getBytes(), QoS.AT_LEAST_ONCE, false);
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }
        message.ack();
    }

    private void initMqtt() {
        try {
//            final Certificate certificate = getCertificate();
//            final TrustManagerFactory trustManagerFactory = getTrustManagerFactory(certificate);
//            final SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

//            mqtt.setSslContext(sslContext);
            mqtt.setConnectAttemptsMax(2);
            mqtt.setHost("localhost", 1883);
            mqtt.setCleanSession(false);
            mqtt.setClientId("WolkiKoki");
//            mqtt.setUserName(device.getSerialId());
//            mqtt.setPassword(device.getPassword());
        } catch (Exception e) {
            System.out.print("Unable to instantiate MQTT.");
        }
    }

}
