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
package com.wolkabout.wolk.filemanagement;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileManagementProtocolTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocolTest.class);
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    MqttClient clientMock;
    @Mock
    FileSystemManagement managementMock;
    FileManagementProtocol protocol;

    @Test
    public void nullCheckMqttClient() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The client cannot be null.");
        new FileManagementProtocol(null, managementMock);
    }

    @Test
    public void nullCheckFileManagement() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The file management cannot be null.");
        new FileManagementProtocol(clientMock, null);
    }

    @Test
    public void createDefaultPath() {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Check that everything is valid
        assertEquals(managementMock, protocol.management);
        assertEquals(clientMock, protocol.client);
    }

    @Test
    public void subscribeTests() throws MqttException {
        // In here, we must check that the protocol will subscribe to each and every topic
        String clientId = "test-client-id";
        doReturn(clientId).when(clientMock).getClientId();
        Map<String, Integer> requiredTopics = new HashMap<String, Integer>() {{
            put(FileManagementProtocol.FILE_UPLOAD_INITIATE, 0);
            put(FileManagementProtocol.FILE_UPLOAD_ABORT, 0);
            put(FileManagementProtocol.FILE_BINARY_RESPONSE, 0);
            put(FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE, 0);
            put(FileManagementProtocol.FILE_URL_DOWNLOAD_ABORT, 0);
            put(FileManagementProtocol.FILE_DELETE, 0);
            put(FileManagementProtocol.FILE_PURGE, 0);
            put(FileManagementProtocol.FILE_LIST_REQUEST, 0);
            put(FileManagementProtocol.FILE_LIST_CONFIRM, 0);
        }};

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);
        protocol.subscribe();

        // Verify all they belong
        verify(clientMock, times(requiredTopics.size()))
                .subscribe(anyString(), anyInt(), any());
        verify(clientMock, times(requiredTopics.size() * 2)).getClientId();
    }
}
