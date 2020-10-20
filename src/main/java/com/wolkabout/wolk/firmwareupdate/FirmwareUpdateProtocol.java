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

import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.FileSystemManagement;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirmwareUpdateProtocol {

    protected static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocol.class);
    protected static final int QOS = 0;
    // Listing all the topics
    protected static final String FIRMWARE_INSTALL_INITIALIZE = "p2d/firmware_update_install/d/";
    protected static final String FIRMWARE_INSTALL_ABORT = "p2d/firmware_update_abort/d/";
    protected static final String FIRMWARE_INSTALL_STATUS = "d2p/firmware_update_status/d/";
    protected static final String FIRMWARE_INSTALL_VERSION = "d2p/firmware_version_update/d/";
    // The main executor
    protected final ExecutorService executor;
    // Given feature classes
    protected final MqttClient client;
    protected final FileSystemManagement management;

    /**
     * This is the default constructor for the FirmwareUpdate feature.
     *
     * @param client     The MQTT client passed by the Wolk instance.
     * @param management The File System management logic that actually interacts with the file system.
     *                   Passed by the Wolk instance.
     */
    public FirmwareUpdateProtocol(MqttClient client, FileSystemManagement management) {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }
        if (management == null) {
            throw new IllegalArgumentException("The file management cannot be null.");
        }

        this.client = client;
        this.management = management;
        this.executor = Executors.newCachedThreadPool();
    }

    public void subscribe() {

    }

    public void publishFirmwareVersion(String version) {

    }
}
