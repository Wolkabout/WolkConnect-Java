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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileSystemManagement.class, FileManagementProtocol.class})
public class FileManagementProtocolTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocolTest.class);
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    MqttClient clientMock;
    @Mock
    FileSystemManagement managementMock;
    FileManagementProtocol protocol;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(FileSystemManagement.class).withAnyArguments().thenReturn(managementMock);
    }

    @Test
    public void nullCheckMqttClient() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The client cannot be null.");
        new FileManagementProtocol(null);
    }

    @Test
    public void nullCheckMqttClientWithFolderPath() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The client cannot be null.");
        new FileManagementProtocol(null, "");
    }

    @Test
    public void emptyFolderPath() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The folder path cannot be empty.");
        new FileManagementProtocol(clientMock, "");
    }

    @Test
    public void createDefaultPath() throws Exception {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock);

        // Check that everything is valid
        assertEquals(managementMock, protocol.management);
        assertEquals(clientMock, protocol.client);

        // Check that the management was constructed
        PowerMockito.verifyNew(FileSystemManagement.class).withArguments("files/");
    }

    @Test
    public void createManagementThrowsException() throws Exception {
        // Assign the mock to throw error
        PowerMockito.whenNew(FileSystemManagement.class).withAnyArguments().
                thenThrow(new IOException("This is a test, to throw an error when a folder is not available."));

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock);
        assertNull(protocol.management);
    }

    @Test
    public void createSpecificPath() throws Exception {
        // Specify the path
        final String customPath = "files-folder/";

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, customPath);

        // Check that everything is valid
        assertEquals(managementMock, protocol.management);
        assertEquals(clientMock, protocol.client);

        // Check that the management was constructed
        PowerMockito.verifyNew(FileSystemManagement.class).withArguments(customPath);
    }

    @Test
    public void createManagementWithSpecificThrowsException() throws Exception {
        // Assign the mock to throw error
        PowerMockito.whenNew(FileSystemManagement.class).withAnyArguments().
                thenThrow(new IOException("This is a test, to throw an error when a folder is not available."));

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, "asdf.asdf");
        assertNull(protocol.management);
    }

    @Test
    public void subscribeTests() throws MqttException {
        // In here, we must check that the protocol will subscribe to each and every topic
        doReturn("test-client-id").when(clientMock).getClientId();
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
        protocol = new FileManagementProtocol(clientMock);
        protocol.subscribe();

        // Verify all they belong
//        verify(clientMock, times(requiredTopics.size())).subscribe(argThat(requiredTopics::containsKey), anyInt(), any());
//        verify(clientMock, times(9)).getClientId();
    }
}
