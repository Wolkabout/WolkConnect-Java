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
package com.wolkabout.wolk.connectivity.mqtt;

import com.wolkabout.wolk.connectivity.AbstractConnectivityService;
import com.wolkabout.wolk.connectivity.model.InboundMessage;
import com.wolkabout.wolk.connectivity.model.OutboundMessage;
import org.fusesource.mqtt.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MqttConnectivityService extends AbstractConnectivityService {
    private static final Logger LOG = LoggerFactory.getLogger(MqttConnectivityService.class);

    private static final int PUBLISH_TIMEOUT_SECONDS = 3;
    private static final QoS QOS = QoS.EXACTLY_ONCE;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
    private ScheduledFuture<?> receiveTask;

    private final MQTT client;
    private final FutureConnection futureConnection;

    private final LinkedBlockingQueue<InboundMessage> inboundMessageQueue = new LinkedBlockingQueue<>();

    public MqttConnectivityService(final MQTT client) {
        this.client = client;
        this.futureConnection = client.futureConnection();

        startInboundMessageDispatcher();
    }

    public MqttConnectivityService(final MQTT client, long connectionAttempts) {
        this.client = client;
        this.client.setConnectAttemptsMax(connectionAttempts);
        this.futureConnection = client.futureConnection();

        startInboundMessageDispatcher();
    }

    private void startInboundMessageDispatcher() {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    listenerOnInboundMessage(inboundMessageQueue.take());
                } catch (InterruptedException e) {
                    LOG.error("Inbound message dispatcher interrupted", e);
                } catch (Exception e) {
                    LOG.warn("Exception in inbound message listener", e);
                }
            }
        }, 0, 5, TimeUnit.MILLISECONDS);
    }

    public synchronized void connect() {
        LOG.info("Connecting to {}", client.getHost());
        futureConnection.connect().then(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Workaround for Fusesource MQTT library - Calling publish immediately after connection establishment fails
                executorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        startReceiveTask();
                        listenerOnConnected();
                    }
                }, 3, TimeUnit.SECONDS);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Failed to connect", throwable);
                listenerOnConnectionFailed();
            }
        });
    }


    public synchronized void disconnect() {
        LOG.info("Disconnecting from {}", client.getHost());
        executorService.shutdown();
        futureConnection.disconnect();
    }

    public synchronized boolean isConnected() {
        return futureConnection.isConnected();
    }

    public synchronized boolean publish(final OutboundMessage outboundMessage) {
        final String channel = outboundMessage.getChannel();
        final String payload = outboundMessage.getPayload();

        LOG.debug("Publishing on channel '{}': {}", channel, payload);
        try {
            futureConnection.publish(channel, payload.getBytes(StandardCharsets.UTF_8), QOS, false)
                    .await(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void subscribe(final String channel) {
        LOG.debug("Subscribing to {}", channel);
        futureConnection.subscribe(new Topic[]{new Topic(channel, QOS)});
    }

    private void startReceiveTask() {
        if (receiveTask == null) {
            receiveTask = executorService.scheduleAtFixedRate((new Runnable() {
                @Override
                public void run() {
                    Message message = null;
                    try {
                        message = futureConnection.receive().await();

                        LOG.debug("Received message on channel {}", message.getTopic());
                        inboundMessageQueue.add(new InboundMessage(message.getTopic(), message.getPayload()));
                    } catch (InterruptedException interrupted) {
                        LOG.info("Receive task interrupted");
                    } catch (Exception e) {
                        LOG.error("error occurred", e);
                    } finally {
                        if (message != null) {
                            message.ack();
                        }
                    }
                }
            }), 0, 5, TimeUnit.MILLISECONDS);
        }
    }
}
