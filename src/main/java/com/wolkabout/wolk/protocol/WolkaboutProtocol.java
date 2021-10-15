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
package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.Attribute;
import com.wolkabout.wolk.model.Feed;
import com.wolkabout.wolk.model.Parameter;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import com.wolkabout.wolk.util.JsonMultivalueSerializer;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class WolkaboutProtocol extends Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(WolkaboutProtocol.class);

    private static final String OUT_DIRECTION = "d2p/";
    private static final String IN_DIRECTION = "p2d/";

    private static final String FEED_VALUES = "/feed_values";
    private static final String FEED_PULL = "/pull_feed_values";
    private static final String PARAMETERS = "/parameters";
    private static final String PARAMETERS_PULL = "/pull_parameters";
    private static final String ATTRIBUTE_REGISTER = "/attribute_registration";
    private static final String TIME = "/time";

    private static final String TIMESTAMP = "utc";

    public WolkaboutProtocol(MqttClient client, FeedHandler feedHandler) {
        super(client, feedHandler);
    }

    @Override
    public void subscribe() throws Exception {
        client.subscribe(IN_DIRECTION + client.getClientId() + FEED_VALUES, QOS, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                LOG.debug("Received on '" + topic + "' payload: " + message.toString());

                List<Feed> feeds = new ArrayList<>();

                try {
                    final HashMap<String, Object>[] inValues = JsonUtil.deserialize(message, HashMap[].class);

                    for (HashMap<String, Object> feed : inValues) {
                        String reference = feed.keySet().stream().filter(s -> !Objects.equals(s, TIMESTAMP)).findFirst().get();

                        Object value = feed.get(reference);

                        if (feed.containsKey(TIMESTAMP)) {
                            long utc = (long) feed.get(TIMESTAMP);

                            feeds.add(new Feed(reference, value.toString(), utc));
                        }
                        else
                        {
                            feeds.add(new Feed(reference, value.toString()));
                        }
                    }

                } catch (Exception e) {
                    LOG.error("Failed to deserialize message from '" + topic + "' payload: " + message.toString());
                    LOG.error(e.getMessage());

                    return;
                }

                feedHandler.onFeedsReceived(feeds);
            }
        });
//
//        client.subscribe(CONFIGURATION_SET + client.getClientId(), QOS, new IMqttMessageListener() {
//            @Override
//            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                final HashMap<String, Object> config = JsonUtil.deserialize(message, HashMap.class);
//                final ConfigurationCommand configurationCommand = new ConfigurationCommand(ConfigurationCommand.CommandType.SET, config);
//
//                configurationHandler.onConfigurationReceived(configurationCommand.getValues());
//
//                publishCurrentConfig();
//            }
//        });
    }

    @Override
    public void publishFeed(Feed feed) {
        publish(OUT_DIRECTION + client.getClientId() + FEED_VALUES, feed);
    }

    @Override
    public void publishFeeds(Collection<Feed> feeds) {
        if (feeds.isEmpty()) {
            return;
        }

        final HashMap<Long, Map<String, Object>> payloadByTime = new HashMap<>();
        for (Feed feed : feeds) {
            if (payloadByTime.containsKey(feed.getUtc())) {
                final Map<String, Object> readingMap = payloadByTime.get(feed.getUtc());
                if (!readingMap.containsKey(feed.getReference())) {
                    readingMap.put(feed.getReference(), JsonMultivalueSerializer.valuesToString(feed.getValues()));
                }
            } else {
                final HashMap<String, Object> readingMap = new HashMap<>();
                readingMap.put("utc", feed.getUtc());
                readingMap.put(feed.getReference(), JsonMultivalueSerializer.valuesToString(feed.getValues()));
                payloadByTime.put(feed.getUtc(), readingMap);
            }
        }

        publish(OUT_DIRECTION + client.getClientId() + FEED_VALUES, new ArrayList<>(payloadByTime.values()));
    }

    @Override
    public void pullFeeds() {
        publish(OUT_DIRECTION + client.getClientId() + FEED_PULL, "");
    }

    @Override
    public void updateParameters(Collection<Parameter> parameters) {
        final HashMap<String, Object> payload = new HashMap<>();

        for (Parameter param : parameters) {
            payload.put(param.getReference(), param.getValue());
        }

        publish(OUT_DIRECTION + client.getClientId() + PARAMETERS, payload);
    }

    @Override
    public void pullParameters() {
        publish(OUT_DIRECTION + client.getClientId() + PARAMETERS_PULL, "");
    }

    @Override
    public void registerAttributes(Collection<Attribute> attributes) {
        final HashMap<String, Map.Entry<String, String>> payload = new HashMap<>();

        for (Attribute attribute : attributes) {
            payload.put(attribute.getName(), new AbstractMap.SimpleImmutableEntry<String, String>(attribute.getDataType().name(), attribute.getValue()));
        }

        publish(OUT_DIRECTION + client.getClientId() + ATTRIBUTE_REGISTER, payload);
    }

    @Override
    public void pullTime() {
        publish(OUT_DIRECTION + client.getClientId() + TIME, "");
    }
}
