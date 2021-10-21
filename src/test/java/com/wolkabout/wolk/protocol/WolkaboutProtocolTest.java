package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.Feed;
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

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void subscribe() throws Exception {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock);
        wolkaboutProtocol.subscribe();
        verify(clientMock, times(3)).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));
    }

    @Test
    public void publishReading() throws MqttException {
        when(clientMock.getClientId())
                .thenReturn("some_key");

        Feed feed = new Feed("reference", "value");
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock);
        wolkaboutProtocol.publishFeed(feed);
        verify(clientMock, atMostOnce()).publish(anyString(), any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishReadings() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, feedHandlerMock, timeHandlerMock, parameterHandlerMock);
        Feed feed = new Feed("reference", "value");
        List<Feed> feeds = new ArrayList<Feed>();
        feeds.add(feed);
        wolkaboutProtocol.publishFeeds(feeds);
        verify(clientMock, atMostOnce()).publish(anyString(), any(byte[].class), anyInt(), anyBoolean());
    }
}