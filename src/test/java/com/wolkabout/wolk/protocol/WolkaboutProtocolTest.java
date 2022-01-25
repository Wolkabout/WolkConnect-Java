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

import com.wolkabout.wolk.model.Feed;
import com.wolkabout.wolk.protocol.handler.ErrorHandler;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import com.wolkabout.wolk.protocol.handler.ParameterHandler;
import com.wolkabout.wolk.protocol.handler.TimeHandler;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class WolkaboutProtocolTest {
    @Mock
    MqttClient clientMock;

    @Mock
    FeedHandler feedHandlerMock;

    @Mock
    TimeHandler timeHandlerMock;

    @Mock
    ParameterHandler parameterHandlerMock;

    @Mock
    ErrorHandler errorHandlerMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void subscribe() throws Exception {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock, errorHandlerMock);
        wolkaboutProtocol.subscribe();
        verify(clientMock, times(4)).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));
    }

    @Test
    public void publishReading() throws MqttException {
        when(clientMock.getClientId())
                .thenReturn("some_key");

        Feed feed = new Feed("reference", "value");
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock, errorHandlerMock);
        wolkaboutProtocol.publishFeed(feed);
        verify(clientMock, atMostOnce()).publish(anyString(), any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishReadings() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock, errorHandlerMock);
        Feed feed = new Feed("reference", "value");
        List<Feed> feeds = new ArrayList<Feed>();
        feeds.add(feed);
        wolkaboutProtocol.publishFeeds(feeds);
        verify(clientMock, atMostOnce()).publish(anyString(), any(byte[].class), anyInt(), anyBoolean());
    }
}
