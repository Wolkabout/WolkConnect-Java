/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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
import com.wolkabout.wolk.model.FeedTemplate;
import com.wolkabout.wolk.model.Parameter;
import com.wolkabout.wolk.protocol.handler.ErrorHandler;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import com.wolkabout.wolk.protocol.handler.ParameterHandler;
import com.wolkabout.wolk.protocol.handler.TimeHandler;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WolkaboutProtocol extends Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(WolkaboutProtocol.class);

    private static final String OUT_DIRECTION = "d2p/";
    private static final String IN_DIRECTION = "p2d/";

    private static final String FEED_VALUES = "/feed_values";
    private static final String FEED_PULL = "/pull_feed_values";
    private static final String FEED_REGISTRATION = "/feed_registration";
    private static final String FEED_REMOVAL = "/feed_removal";
    private static final String PARAMETERS = "/parameters";
    private static final String PARAMETERS_PULL = "/pull_parameters";
    private static final String PARAMETERS_SYNC = "/synchronize_parameters";
    private static final String ATTRIBUTE_REGISTER = "/attribute_registration";
    private static final String TIME = "/time";
    private static final String ERROR = "/error";

    private static final String TIMESTAMP = "utc";

    public WolkaboutProtocol(MqttClient client, FeedHandler feedHandler, TimeHandler timeHandler, ParameterHandler parameterHandler, ErrorHandler errorHandler) {
        super(client, feedHandler, timeHandler, parameterHandler, errorHandler);
    }

    @Override
    public void subscribe() throws Exception {
        LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FEED_VALUES + "'.");
        client.subscribe(IN_DIRECTION + client.getClientId() + FEED_VALUES, QOS, this::handleFeedValues);

        LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + PARAMETERS + "'.");
        client.subscribe(IN_DIRECTION + client.getClientId() + PARAMETERS, QOS, this::handleParameters);

        LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + TIME + "'.");
        client.subscribe(IN_DIRECTION + client.getClientId() + TIME, QOS, this::handleTime);

        LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + ERROR + "'.");
        client.subscribe(IN_DIRECTION + client.getClientId() + ERROR, QOS, this::handleError);
    }

    @Override
    public void publishFeed(Feed feed) {
        final Map<String, Object> feedMap = new HashMap<>();
        feedMap.put(feed.getReference(), feed.getValues().size() > 1 ? feed.getValues() : feed.getValue());
        feedMap.put("utc", feed.getUtc());

        publish(OUT_DIRECTION + client.getClientId() + FEED_VALUES, feedMap);
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
                    readingMap.put(feed.getReference(), feed.getValues().size() > 1 ? feed.getValues() : feed.getValue());
                }
            } else {
                final HashMap<String, Object> readingMap = new HashMap<>();
                readingMap.put("utc", feed.getUtc());

                readingMap.put(feed.getReference(), feed.getValues().size() > 1 ? feed.getValues() : feed.getValue());
                payloadByTime.put(feed.getUtc(), readingMap);
            }
        }

        publish(OUT_DIRECTION + client.getClientId() + FEED_VALUES, payloadByTime.values());
    }

    @Override
    public void registerFeeds(Collection<FeedTemplate> feeds) {
        publish(OUT_DIRECTION + client.getClientId() + FEED_REGISTRATION, feeds);
    }

    @Override
    public void removeFeeds(Collection<String> feedReferences) {
        publish(OUT_DIRECTION + client.getClientId() + FEED_REMOVAL, feedReferences);
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
    public void syncronizeParameters(Collection<String> parameterNames) {
        publish(OUT_DIRECTION + client.getClientId() + PARAMETERS_SYNC, parameterNames);
    }

    @Override
    public void registerAttributes(Collection<Attribute> attributes) {
        publish(OUT_DIRECTION + client.getClientId() + ATTRIBUTE_REGISTER, attributes);
    }

    @Override
    public void pullTime() {
        publish(OUT_DIRECTION + client.getClientId() + TIME, "");
    }

    private void handleFeedValues(String topic, MqttMessage message) {
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
                } else {
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

    private void handleParameters(String topic, MqttMessage message) {
        LOG.debug("Received on '" + topic + "' payload: " + message.toString());

        List<Parameter> parameters = new ArrayList<>();

        try {
            final HashMap<String, Object> inValues = JsonUtil.deserialize(message, HashMap.class);

            for (String key : inValues.keySet()) {
                parameters.add(new Parameter(key, inValues.get(key)));
            }
        } catch (Exception e) {
            LOG.error("Failed to deserialize message from '" + topic + "' payload: " + message.toString());
            LOG.error(e.getMessage());

            return;
        }

        parameterHandler.onParametersReceived(parameters);
    }

    private void handleTime(String topic, MqttMessage message) {
        LOG.debug("Received on '" + topic + "' payload: " + message.toString());

        long timestamp;

        try {
            timestamp = Long.parseLong(message.toString());
        } catch (Exception e) {
            LOG.error("Failed to deserialize message from '" + topic + "' payload: " + message.toString());
            LOG.error(e.getMessage());

            return;
        }

        timeHandler.onTimeReceived(timestamp);
    }

    private void handleError(String topic, MqttMessage message) {
        LOG.debug("Received on '" + topic + "' payload: " + message.toString());

        errorHandler.onErrorReceived(message.toString());
    }
}
