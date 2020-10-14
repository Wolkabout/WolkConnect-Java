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

import com.wolkabout.wolk.filemanagement.model.device2platform.*;
import com.wolkabout.wolk.filemanagement.model.platform2device.*;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileManagementProtocol {

    protected static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocol.class);
    protected static final int QOS = 0;
    // File upload initiation and input/output topics
    protected static final String FILE_UPLOAD_INITIATE = "p2d/file_upload_initiate/d/";
    protected static final String FILE_UPLOAD_STATUS = "d2p/file_upload_status/d/";
    protected static final String FILE_UPLOAD_ABORT = "p2d/file_upload_abort/d/";
    // File upload chunk topics
    protected static final String FILE_BINARY_REQUEST = "d2p/file_binary_request/d/";
    protected static final String FILE_BINARY_RESPONSE = "p2d/file_binary_response/d/";
    // File URL download initiation and input/output topics
    protected static final String FILE_URL_DOWNLOAD_INITIATE = "p2d/file_url_download_initiate/d/";
    protected static final String FILE_URL_DOWNLOAD_STATUS = "d2p/file_url_download_status/d/";
    protected static final String FILE_URL_DOWNLOAD_ABORT = "p2d/file_url_download_abort/d/";
    // File removal topics
    protected static final String FILE_DELETE = "p2d/file_delete/d/";
    protected static final String FILE_PURGE = "p2d/file_purge/d/";
    // File list input/output topics
    protected static final String FILE_LIST_REQUEST = "p2d/file_list_request/d/";
    protected static final String FILE_LIST_RESPONSE = "d2p/file_list_response/d/";
    protected static final String FILE_LIST_UPDATE = "d2p/file_list_update/d/";
    protected static final String FILE_LIST_CONFIRM = "p2d/file_list_confirm/d/";
    // The MQTT client
    protected final MqttClient client;
    // The Executor
    protected final ExecutorService executor;
    // The feature classes for functionality
    protected FileSystemManagement management;
    protected FileDownloadSession fileDownloadSession;
    protected UrlFileDownloadSession urlFileDownloadSession;

    /**
     * This is the constructor for the FileManagement feature. This will create the file system folder
     * by default `<work_dir>/files/`
     *
     * @param client The MQTT client passed to by the Wolk instance.
     */
    public FileManagementProtocol(MqttClient client) throws IOException {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }

        this.client = client;
        this.management = new FileSystemManagement("files/");
        LOG.debug("Created FileSystemManagement with folder path: './files/'");

        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * This is the constructor for the FileManagement feature. This allows the user to set a custom folder path
     * for storing files.
     *
     * @param client     The MQTT client passed to by the Wolk instance.
     * @param folderPath The custom folder path for file storing.
     * @throws IOException If folder does not exist, and cannot be made, this exception will be thrown.
     */
    public FileManagementProtocol(MqttClient client, String folderPath) throws IOException {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }
        if (folderPath.isEmpty()) {
            throw new IllegalArgumentException("The folder path cannot be empty.");
        }

        this.client = client;
        this.management = new FileSystemManagement(folderPath);
        LOG.debug("Created FileSystemManagement with folder path: '" + folderPath + "'");

        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * This is the method that is used to capture the file list and send it.
     */
    public void publishFileList() {
        publishFileList(FILE_LIST_UPDATE + client.getClientId());
    }

    /**
     * This is the main method that uses the passed MqttClient to subscribe to all the topics
     * that need to be subscribed to by the protocol.
     */
    public void subscribe() {
        try {
            // File transfer subscriptions
            LOG.debug("Subscribing to topic '" + FILE_UPLOAD_INITIATE + client.getClientId() + "'.");
            client.subscribe(FILE_UPLOAD_INITIATE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferInitiation(topic, message)));
            LOG.debug("Subscribing to topic '" + FILE_UPLOAD_ABORT + client.getClientId() + "'.");
            client.subscribe(FILE_UPLOAD_ABORT + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferAbort(topic, message)));
            LOG.debug("Subscribing to topic '" + FILE_BINARY_RESPONSE + client.getClientId() + "'.");
            client.subscribe(FILE_BINARY_RESPONSE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferBinaryResponse(topic, message)));
            // File URL download subscriptions
            LOG.debug("Subscribing to topic '" + FILE_URL_DOWNLOAD_INITIATE + client.getClientId() + "'.");
            client.subscribe(FILE_URL_DOWNLOAD_INITIATE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleUrlDownloadInitiation(topic, message)));
            LOG.debug("Subscribing to topic '" + FILE_URL_DOWNLOAD_ABORT + client.getClientId() + "'.");
            client.subscribe(FILE_URL_DOWNLOAD_ABORT + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleUrlDownloadAbort(topic, message)));
            // File deletion subscriptions
            LOG.debug("Subscribing to topic '" + FILE_DELETE + client.getClientId() + "'.");
            client.subscribe(FILE_DELETE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFileDeletion(topic, message)));
            LOG.debug("Subscribing to topic '" + FILE_PURGE + client.getClientId() + "'.");
            client.subscribe(FILE_PURGE + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFilePurge(topic, message)));
            // File list subscriptions
            LOG.debug("Subscribing to topic '" + FILE_LIST_REQUEST + client.getClientId() + "'.");
            client.subscribe(FILE_LIST_REQUEST + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> handleFileListRequest(topic, message)));
            LOG.debug("Subscribing to topic '" + FILE_LIST_CONFIRM + client.getClientId() + "'.");
            client.subscribe(FILE_LIST_CONFIRM + client.getClientId(), QOS,
                    (topic, message) -> executor.execute(() -> logReceivedMqttMessage(topic, message)));
        } catch (MqttException exception) {
            LOG.error(exception.getMessage());
        }
    }

    private void handleFileTransferInitiation(String topic, MqttMessage message) {
        try {
            logReceivedMqttMessage(topic, message);
            // If a session is already running, that means the initialization message is not acceptable now.
            if (isSessionRunning()) {
                logReceivedMqttMessage(topic, message);
                LOG.warn("File transfer session is already ongoing. Ignoring this message...");
                return;
            }

            // Parse the initialization message
            FileInit initMessage = JsonUtil.deserialize(message, FileInit.class);

            // Start the session
            fileDownloadSession = new FileDownloadSession(initMessage, new FileDownloadSession.Callback() {
                @Override
                public void sendRequest(String fileName, int chunkIndex, int chunkSize) {
                    handleFileTransferRequest(fileName, chunkIndex, chunkSize);
                }

                @Override
                public void onFinish(FileTransferStatus status, FileTransferError error) {
                    handleFileTransferFinish(fileDownloadSession, status, error);
                    fileDownloadSession = null;
                }
            });

            // Send the transferring message
            publish(FILE_UPLOAD_STATUS + client.getClientId(), new FileStatus(initMessage.getFileName(),
                    FileTransferStatus.FILE_TRANSFER));
        } catch (Exception exception) {
            LOG.error("Error occurred during handling of file transfer initializer message: " + exception);
        }
    }

    private void handleFileTransferAbort(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (fileDownloadSession == null) {
            LOG.warn("Received binary chunk data when session is not ongoing.");
            return;
        }

        // Parse the payload and check its validity
        FileAbort abortMessage = JsonUtil.deserialize(message, FileAbort.class);
        if (!abortMessage.getFileName().equals(fileDownloadSession.getInitMessage().getFileName())) {
            LOG.warn("Received file transfer abort message with non-matching file name.");
            return;
        }

        // Abort the session
        fileDownloadSession.abort();
    }

    private void handleFileTransferBinaryResponse(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (fileDownloadSession == null) {
            LOG.warn("Received binary chunk data when session is not ongoing.");
            return;
        }

        // Pass on the payload
        fileDownloadSession.receiveBytes(message.getPayload());
    }

    private void handleFileTransferRequest(String fileName, int chunkIndex, int chunkSize) {
        // Create a message to request the data and send it
        ChunkRequest chunkRequest = new ChunkRequest(fileName, chunkIndex, chunkSize);
        publish(FILE_BINARY_REQUEST + client.getClientId(), chunkRequest);
    }

    private void handleFileTransferFinish(FileDownloadSession session, FileTransferStatus status,
                                          FileTransferError error) {
        // Null check what needs to be null checked
        if (session == null) {
            throw new IllegalStateException("Handle file session finish is called with a null session.");
        }
        if (status == null) {
            throw new IllegalStateException("Handle file session finish is called with a null status.");
        }

        // Announce the not good status
        if (status != FileTransferStatus.FILE_READY) {
            FileStatus statusMessage = new FileStatus(session.getInitMessage().getFileName(), status, error);
            publish(FILE_UPLOAD_STATUS + client.getClientId(), statusMessage);
            return;
        }

        // Announce the status for good status, and save the data from file, and publish the file list.
        FileStatus statusMessage = new FileStatus(session.getInitMessage().getFileName(), status);
        publish(FILE_UPLOAD_STATUS + client.getClientId(), statusMessage);

        // Make the file
        management.createFile(session.getBytes(), session.getInitMessage().getFileName());
        publishFileList();
    }

    /**
     * This is the method that defines the behaviour when a FILE_URL_DOWNLOAD_INITIATE message is received.
     */
    private void handleUrlDownloadInitiation(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // If a session is already running, that means the initialization message is not acceptable now.
        if (isSessionRunning()) {
            logReceivedMqttMessage(topic, message);
            LOG.warn("File transfer session is already ongoing. Ignoring this message...");
            return;
        }

        // Parse the initialization message
        UrlInfo urlInit = JsonUtil.deserialize(message, UrlInfo.class);

        // Give the transfer message
        publish(FILE_URL_DOWNLOAD_STATUS + client.getClientId(),
                new UrlStatus(urlInit.getFileUrl(), FileTransferStatus.FILE_TRANSFER));

        // Create the session
        urlFileDownloadSession = new UrlFileDownloadSession(urlInit, (status, error) -> {
            handleUrlSessionFinish(urlFileDownloadSession, status, error);
            urlFileDownloadSession = null;
        });
    }

    /**
     * This is the method that defines the behaviour when a FILE_URL_DOWNLOAD_ABORT message is received.
     */
    private void handleUrlDownloadAbort(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (urlFileDownloadSession == null) {
            LOG.warn("Received URL download abort while session is not running.");
            return;
        }

        // Parse the payload, and check its validity
        UrlAbort abortMessage = JsonUtil.deserialize(message, UrlAbort.class);
        if (!abortMessage.getFileUrl().equals(urlFileDownloadSession.getInitMessage().getFileUrl())) {
            LOG.warn("Received URL download abort for non-matching URL paths.");
            return;
        }

        // Abort the message
        urlFileDownloadSession.abort();
    }

    /**
     * This is the callback method for the URL Download session to handle the result of the session.
     */
    private void handleUrlSessionFinish(UrlFileDownloadSession session, FileTransferStatus status,
                                        FileTransferError error) {
        // Null check what needs to be null checked
        if (session == null) {
            throw new IllegalStateException("Handle URL session finish is called with a null session.");
        }
        if (status == null) {
            throw new IllegalStateException("Handle URL session finish is called with a null status.");
        }

        // Announce the not good status
        if (status != FileTransferStatus.FILE_READY) {
            UrlStatus statusMessage = new UrlStatus(session.getInitMessage().getFileUrl(), status, error);
            publish(FILE_URL_DOWNLOAD_STATUS + client.getClientId(), statusMessage);
            return;
        }

        // Announce the status for good status, and save the data from file, and publish the file list now.
        UrlStatus statusMessage = new UrlStatus(session.getInitMessage().getFileUrl(), FileTransferStatus.FILE_READY,
                session.getFileName());
        publish(FILE_URL_DOWNLOAD_STATUS + client.getClientId(), statusMessage);

        // Make the file
        management.createFile(session.getFileData(), session.getFileName());
        publishFileList();
    }

    /**
     * This is the method that defines the behaviour when a FILE_DELETE message is received.
     */
    private void handleFileDeletion(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        FileDelete fileDelete = JsonUtil.deserialize(message, FileDelete.class);
        management.deleteFile(fileDelete.getFileName());
    }

    /**
     * This is the method that defines the behaviour when a FILE_PURGE message is received.
     */
    private void handleFilePurge(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        management.purgeDirectory();
    }

    /**
     * This is the method that defines the behaviour when a FILE_LIST_REQUEST message is received.
     */
    private void handleFileListRequest(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        publishFileList(FILE_LIST_RESPONSE + client.getClientId());
    }

    /**
     * This is the method that is used to capture the file list and send it.
     * This one takes in a topic to which the message will be sent.
     *
     * @param topic The topic to which the message will be sent.
     */
    private void publishFileList(String topic) {
        // Acquire all the files
        List<String> files = management.listAllFiles();
        LOG.trace("Peeked the file system to find files, found " + files.size() + " files.");

        // Place them all in the payload
        List<FileInformation> payload = new ArrayList<>();
        for (String file : files) {
            payload.add(new FileInformation(file));
        }
        LOG.trace("Created payload to announce '" + payload + "'.");

        // Send everything
        publish(topic, payload);
    }

    /**
     * This is a utility method that is meant to just log a received message.
     */
    private void logReceivedMqttMessage(String topic, MqttMessage message) {
        if (message.getPayload().length > 1000) {
            LOG.debug("Received '" + topic + "' -> " + message.getPayload().length + " bytes.");
        } else {
            LOG.debug("Received '" + topic + "' -> " + message.toString() + ".");
        }
    }

    /**
     * This is a utility method that returns whether or not some session is ongoing.
     *
     * @return Returns true if some session is ongoing.
     */
    private boolean isSessionRunning() {
        LOG.trace("FileDownloadSession: " + (fileDownloadSession != null) + "\n" +
                "UrlFileDownloadSession: " + (urlFileDownloadSession != null));
        return fileDownloadSession != null || urlFileDownloadSession != null;
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
