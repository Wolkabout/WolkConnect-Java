package com.wolkabout.wolk.protocol;

import com.wolkabout.wolk.model.ActuatorStatus;
import com.wolkabout.wolk.model.Alarm;
import com.wolkabout.wolk.model.Configuration;
import com.wolkabout.wolk.model.Reading;
import com.wolkabout.wolk.protocol.WolkaboutProtocol;
import com.wolkabout.wolk.protocol.handler.ActuatorHandler;
import com.wolkabout.wolk.protocol.handler.ConfigurationHandler;
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
    ActuatorHandler actuatorHandlerMock;

    @Mock
    ConfigurationHandler configurationHandlerMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void subscribe() throws Exception {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        wolkaboutProtocol.subscribe();
        verify(clientMock, times(3)).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));
    }

    @Test
    public void publishReading() throws MqttException {
        Reading reading = new Reading("reference", "value");
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        wolkaboutProtocol.publishReading(reading);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishReadings() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        Reading reading = new Reading("reference", "value");
        List<Reading> readings = new ArrayList<Reading>();
        readings.add(reading);
        wolkaboutProtocol.publishReadings(readings);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishAlarm() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        Alarm alarm = new Alarm("reference", false);
        wolkaboutProtocol.publishAlarm(alarm);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishAlarms() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        Alarm alarm = new Alarm("reference", false);
        List<Alarm> alarms = new ArrayList<Alarm>();
        alarms.add(alarm);
        wolkaboutProtocol.publishAlarms(alarms);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishConfiguration() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        Configuration configuration = new Configuration("reference", "value");
        List<Configuration> configurations = new ArrayList<Configuration>();
        configurations.add(configuration);
        wolkaboutProtocol.publishConfiguration(configurations);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }

    @Test
    public void publishActuatorStatus() throws MqttException {
        WolkaboutProtocol wolkaboutProtocol = new WolkaboutProtocol(clientMock, actuatorHandlerMock, configurationHandlerMock);
        ActuatorStatus actuatorStatus = new ActuatorStatus(ActuatorStatus.Status.READY, "value", "reference");
        wolkaboutProtocol.publishActuatorStatus(actuatorStatus);
        verify(clientMock, atMostOnce()).publish(anyString(),any(byte[].class), anyInt(), anyBoolean());
    }
}