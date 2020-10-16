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

import static org.junit.Assert.assertEquals;

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
        FileManagementProtocol protocol = new FileManagementProtocol(clientMock);

        // Check that everything is valid
        assertEquals(managementMock, protocol.management);
        assertEquals(clientMock, protocol.client);

        // Check that the management was constructed
        PowerMockito.verifyNew(FileSystemManagement.class).withArguments("files/");
    }
}
