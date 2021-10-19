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
import com.wolkabout.wolk.model.FeedTemplate;
import com.wolkabout.wolk.model.Parameter;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import com.wolkabout.wolk.protocol.handler.ParameterHandler;
import com.wolkabout.wolk.protocol.handler.TimeHandler;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public abstract class Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(Protocol.class);

    protected final MqttClient client;
    protected final FeedHandler feedHandler;
    protected final TimeHandler timeHandler;
    protected final ParameterHandler parameterHandler;

    protected static final int QOS = 2;

    public Protocol(MqttClient client, FeedHandler feedHandler, TimeHandler timeHandler, ParameterHandler parameterHandler) {
        this.client = client;
        this.feedHandler = feedHandler;
        this.timeHandler = timeHandler;
        this.parameterHandler = parameterHandler;
    }

    public abstract void subscribe() throws Exception;

    protected void publish(String topic, Object payload) {
        try {
            LOG.debug("Publishing to '" + topic + "' payload: " + new String(JsonUtil.serialize(payload), StandardCharsets.UTF_8));
            client.publish(topic, JsonUtil.serialize(payload), QOS, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    public abstract void publishFeed(Feed feed);

    public abstract void publishFeeds(Collection<Feed> feeds);

    public abstract void registerFeeds(Collection<FeedTemplate> feeds);

    public abstract void removeFeeds(Collection<String> feedReferences);

    public abstract void pullFeeds();

    public abstract void updateParameters(Collection<Parameter> parameters);

    public abstract void pullParameters();

    public abstract void registerAttributes(Collection<Attribute> attributes);

    public abstract void pullTime();
}
