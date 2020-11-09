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
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateError;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateStatus;
import com.wolkabout.wolk.firmwareupdate.model.device2platform.UpdateStatus;
import com.wolkabout.wolk.firmwareupdate.model.platform2device.UpdateInit;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirmwareUpdateProtocol {

    protected static final Logger LOG = LoggerFactory.getLogger(FirmwareUpdateProtocol.class);
    protected static final int QOS = 0;
    protected static final String FIRMWARE_VERSION_FILE = "lastFirmwareVersion.txt";
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
    protected final FirmwareInstaller installer;
    protected FirmwareUpdateStatus lastSentStatus;

    /**
     * This is the default constructor for the FirmwareUpdate feature.
     *
     * @param client     The MQTT client passed by the Wolk instance.
     * @param management The File System management logic that actually interacts with the file system.
     *                   Passed by the Wolk instance.
     */
    public FirmwareUpdateProtocol(MqttClient client, FileSystemManagement management,
                                  FirmwareInstaller installer) {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }
        if (management == null) {
            throw new IllegalArgumentException("The file management cannot be null.");
        }
        if (installer == null) {
            throw new IllegalArgumentException("The firmware installer cannot be null.");
        }

        this.client = client;
        this.management = management;
        this.installer = installer;
        // Inject ourselves into the installer
        this.executor = Executors.newCachedThreadPool();
    }

    public boolean checkFirmwareVersion() {
        // Logic for version tracking to report behaviour
        if (this.management.fileExists(FIRMWARE_VERSION_FILE)) {
            LOG.debug("Detected a firmware version file.");
            String loadedVersion;
            if ((loadedVersion = loadVersionFromFile()) != null) {
                if (!loadedVersion.equals(installer.onFirmwareVersion())) {
                    LOG.debug("Detected firmware versions are different. Firmware installation was successful.");
                    sendStatusMessage(FirmwareUpdateStatus.COMPLETED);
                } else {
                    LOG.debug("Detected firmware versions are the same. Firmware installation has failed.");
                    sendErrorMessage(FirmwareUpdateError.INSTALLATION_FAILED);
                }
            }
            this.management.deleteFile(FIRMWARE_VERSION_FILE);
            return true;
        } else {
            return false;
        }
    }

    private void saveVersionToFile(String version) {
        try {
            management.createFile(version.getBytes(), FIRMWARE_VERSION_FILE);
        } catch (IOException exception) {
            LOG.error("Failed to save firmware version to persistence for reporting.");
        }
    }

    private String loadVersionFromFile() {
        try {
            return Files.readAllLines(management.getFile(FIRMWARE_VERSION_FILE).toPath()).get(0);
        } catch (IOException exception) {
            LOG.error("Failed to load firmware version from persistence for reporting.");
            return null;
        }
    }

    private void removeVersionFile() {
        management.deleteFile(FIRMWARE_VERSION_FILE);
    }

    /**
     * This is the main method that uses the passed MqttClient to subscribe to all the topics
     * that need to be subscribed to by the protocol.
     */
    public void subscribe() {
        try {
            // Initialization subscription
            LOG.debug("Subscribing to topic '" + FIRMWARE_INSTALL_INITIALIZE + client.getClientId() + "'.");
            client.subscribe(FIRMWARE_INSTALL_INITIALIZE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFirmwareUpdateInitiation(topic, message)));
            // Abort subscription
            LOG.debug("Subscribing to topic '" + FIRMWARE_INSTALL_ABORT + client.getClientId() + "'.");
            client.subscribe(FIRMWARE_INSTALL_ABORT + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFirmwareUpdateAbort(topic, message)));
        } catch (MqttException exception) {
            LOG.error(exception.getMessage());
        }
    }

    void handleFirmwareUpdateInitiation(String topic, MqttMessage message) {
        // Log the message
        logReceivedMqttMessage(topic, message);

        // Parse the payload
        UpdateInit init = JsonUtil.deserialize(message, UpdateInit.class);

        // Check that the file actually exists
        if (!management.fileExists(init.getFileName())) {
            LOG.error("Received firmware update init message for file that does not exist '" + init.getFileName() + "'.");
            sendErrorMessage(FirmwareUpdateError.FILE_NOT_PRESENT);
            return;
        }

        // Check that it is readable
        if (new File(init.getFileName()).canRead()) {
            LOG.error("Received firmware update init message for unreadable file '" + init.getFileName() + "'.");
            sendErrorMessage(FirmwareUpdateError.FILE_SYSTEM_ERROR);
            return;
        }

        // Call the installer
        LOG.info("Received firmware update message for file '" + init.getFileName() + "'.");
        sendStatusMessage(FirmwareUpdateStatus.INSTALLATION);
        final String version = installer.onFirmwareVersion();
        LOG.info("Firmware update installation ongoing. Saving version '" + version + "'.");
        saveVersionToFile(version);
        if (!installer.onInstallCommandReceived(init.getFileName())
                && lastSentStatus == FirmwareUpdateStatus.INSTALLATION) {
            LOG.warn("Firmware update installation failed by user.");
            sendErrorMessage(FirmwareUpdateError.INSTALLATION_FAILED);
            removeVersionFile();
        }
    }

    void handleFirmwareUpdateAbort(String topic, MqttMessage message) {
        // Log the message
        logReceivedMqttMessage(topic, message);

        // Delete the version file if it exists
        removeVersionFile();

        // Call the installer
        sendStatusMessage(FirmwareUpdateStatus.ABORTED);
        installer.onAbortCommandReceived();
    }

    public void sendStatusMessage(FirmwareUpdateStatus status) {
        lastSentStatus = status;
        publish(FIRMWARE_INSTALL_STATUS + client.getClientId(), new UpdateStatus(status));
    }

    public void sendErrorMessage(FirmwareUpdateError error) {
        lastSentStatus = FirmwareUpdateStatus.ERROR;
        publish(FIRMWARE_INSTALL_STATUS + client.getClientId(), new UpdateStatus(error));
    }

    public void publishFirmwareVersion(String version) {
        String topic = FIRMWARE_INSTALL_VERSION + client.getClientId();

        try {
            LOG.debug("Publishing to '" + topic + "' payload: '" + version + "'");
            client.publish(topic, version.getBytes(), QOS, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " +
                    topic + " with payload: '" + version + "'", e);
        }
    }

    /**
     * This is a utility method that is meant to just log a received message.
     */
    private void logReceivedMqttMessage(String topic, MqttMessage message) {
        LOG.debug("Received '" + topic + "' -> " + message.toString() + ".");
    }

    /**
     * This is an internal method used to publish a message to the MQTT broker.
     *
     * @param topic   Topic to which the message is being sent.
     * @param payload This is the object payload that will be parsed into JSON and sent.
     */
    private void publish(String topic, Object payload) {
        try {
            LOG.debug("Publishing to '" + topic + "' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), QOS, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }
}
