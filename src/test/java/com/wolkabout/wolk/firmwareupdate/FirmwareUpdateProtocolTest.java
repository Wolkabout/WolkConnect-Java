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
package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.filemanagement.FileSystemManagement;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateError;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateStatus;
import com.wolkabout.wolk.firmwareupdate.model.platform2device.UpdateInit;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FirmwareUpdateProtocolTest {

    private static final Logger LOG = LoggerFactory.getLogger(FirmwareUpdateProtocolTest.class);
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    FirmwareUpdateProtocol protocol;

    @Mock
    File fileMock;
    @Mock
    MqttClient clientMock;
    @Mock
    FileSystemManagement managementMock;
    @Mock
    FirmwareInstaller installerMock;

    @Test
    public void nullCheckMqttClient() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The client cannot be null.");
        new FirmwareUpdateProtocol(null, managementMock, installerMock);
    }

    @Test
    public void nullCheckFileManagement() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The file management cannot be null.");
        new FirmwareUpdateProtocol(clientMock, null, installerMock);
    }

    @Test
    public void nullCheckFirmwareInstaller() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The firmware installer cannot be null.");
        new FirmwareUpdateProtocol(clientMock, managementMock, null);
    }

    @Test
    public void testSubscribeThrows() throws MqttException {
        doThrow(new MqttException(new Exception("Test error message."))).when(clientMock)
                .subscribe(anyString(), anyInt(), any());

        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);
        protocol.subscribe();
    }

    @Test
    public void testSubscribe() throws MqttException {
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);
        protocol.subscribe();

        verify(clientMock, times(2)).subscribe(anyString(), anyInt(), any());
    }

    @Test
    public void initializationMessageNoFile() throws MqttException, InterruptedException {
        // Setup the return value
        when(managementMock.fileExists(anyString())).thenReturn(false);

        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Setup the test init message
        UpdateInit initMessage = new UpdateInit(new String[]{clientMock.getClientId()}, "whatever-file");

        // Do the call
        protocol.handleFirmwareUpdateInitiation(
                FirmwareUpdateProtocol.FIRMWARE_INSTALL_INITIALIZE + clientMock.getClientId(),
                new MqttMessage(JsonUtil.serialize(initMessage)));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Check all the mock calls
        verify(managementMock, times(1)).fileExists(anyString());
        verify(clientMock, times(3)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void initializationHappyFlow() throws MqttException, InterruptedException {
        // Setup the return of the mock
        when(managementMock.fileExists("whatever-file")).thenReturn(true);
        when(installerMock.onFirmwareVersion()).thenReturn("Version1");

        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Setup the test init message
        UpdateInit initMessage = new UpdateInit(new String[]{clientMock.getClientId()}, "whatever-file");

        // Do the call
        protocol.handleFirmwareUpdateInitiation(
                FirmwareUpdateProtocol.FIRMWARE_INSTALL_INITIALIZE + clientMock.getClientId(),
                new MqttMessage(JsonUtil.serialize(initMessage)));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Check all the mock calls
        verify(managementMock, times(1)).fileExists(anyString());
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
        verify(installerMock, times(1))
                .onInstallCommandReceived(eq("whatever-file"));
    }

    @Test
    public void abortMessageHappyFlow() {
        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Call the method
        protocol.handleFirmwareUpdateAbort(FirmwareUpdateProtocol.FIRMWARE_INSTALL_ABORT, new MqttMessage(new byte[0]));

        // Verify the mock call
        verify(installerMock, times(1)).onAbortCommandReceived();
    }

    @Test
    public void publishTransferStatusHappyFlow() throws MqttException {
        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        protocol.sendStatusMessage(FirmwareUpdateStatus.INSTALLATION);

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void publishTransferStatusPublishThrows() throws MqttException {
        // Setup the throwing method
        doThrow(new MqttException(new Exception("Test MQTT exception."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        exceptionRule.expect(IllegalArgumentException.class);
        protocol.sendStatusMessage(FirmwareUpdateStatus.INSTALLATION);

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
    }

    @Test
    public void publishTransferErrorHappyFlow() throws MqttException {
        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        protocol.sendErrorMessage(FirmwareUpdateError.UNSPECIFIED_ERROR);

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void publishTransferErrorPublishThrows() throws MqttException {
        // Setup the throwing method
        doThrow(new MqttException(new Exception("Test MQTT exception."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        exceptionRule.expect(IllegalArgumentException.class);
        protocol.sendErrorMessage(FirmwareUpdateError.UNSPECIFIED_ERROR);

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
    }

    @Test
    public void publishFirmwareVersionHappyFlow() throws MqttException {
        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        protocol.publishFirmwareVersion("Firmware 1.0.0");

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void publishFirmwareVersionPublishThrows() throws MqttException {
        // Setup the throwing method
        doThrow(new MqttException(new Exception("Test MQTT exception."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Setup the protocol
        protocol = new FirmwareUpdateProtocol(clientMock, managementMock, installerMock);

        // Send the firmware version
        exceptionRule.expect(IllegalArgumentException.class);
        protocol.publishFirmwareVersion("Firmware 1.0.0");

        // Verify all the mock calls
        verify(clientMock, times(1)).getClientId();
    }
}
