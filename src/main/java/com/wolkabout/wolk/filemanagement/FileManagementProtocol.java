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

import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.device2platform.ChunkRequest;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileInformation;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileStatus;
import com.wolkabout.wolk.filemanagement.model.device2platform.UrlStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.*;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileManagementProtocol {

    protected static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocol.class);
    protected static final int QOS = 2;

    protected static final String OUT_DIRECTION = "d2p/";
    protected static final String IN_DIRECTION = "p2d/";

    // File upload initiation and input/output topics
    protected static final String FILE_UPLOAD_INITIATE = "/file_upload_initiate";
    protected static final String FILE_UPLOAD_STATUS = "/file_upload_status";
    protected static final String FILE_UPLOAD_ABORT = "/file_upload_abort";
    // File upload chunk topics
    protected static final String FILE_BINARY_REQUEST = "/file_binary_request";
    protected static final String FILE_BINARY_RESPONSE = "/file_binary_response";
    // File URL download initiation and input/output topics
    protected static final String FILE_URL_DOWNLOAD_INITIATE = "/file_url_download_initiate";
    protected static final String FILE_URL_DOWNLOAD_STATUS = "/file_url_download_status";
    protected static final String FILE_URL_DOWNLOAD_ABORT = "/file_url_download_abort";
    // File removal topics
    protected static final String FILE_DELETE = "/file_delete";
    protected static final String FILE_PURGE = "/file_purge";
    // File list input/output topics
    protected static final String FILE_LIST = "/file_list";
    // The MQTT client
    protected final MqttClient client;
    // The Executor
    protected final ExecutorService executor;
    // The feature classes for functionality
    protected final FileSystemManagement management;
    protected FileDownloadSession fileDownloadSession;
    protected UrlFileDownloadSession urlFileDownloadSession;
    private final UrlFileDownloader urlFileDownloader;
    protected int maxChunkSize = 0;

    /**
     * This is the constructor for the FileManagement feature.
     *
     * @param client     The MQTT client passed to by the Wolk instance.
     * @param management The File System management logic that actually interacts with the file system.
     *                   Passed by the Wolk instance.
     */
    public FileManagementProtocol(MqttClient client, FileSystemManagement management) {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }
        if (management == null) {
            throw new IllegalArgumentException("The file management cannot be null.");
        }

        this.client = client;
        this.management = management;
        this.urlFileDownloader = null;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * This is the constructor for the FileManagement feature.
     * This one does allow the custom url file downloader implementation.
     *
     * @param client            The MQTT client passed to by the Wolk instance.
     * @param management        The File System management logic that actually interacts with the file system.
     *                          Passed by the Wolk instance.
     * @param urlFileDownloader The custom URL file downloader implementation.
     */
    public FileManagementProtocol(MqttClient client, FileSystemManagement management,
                                  UrlFileDownloader urlFileDownloader) {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null.");
        }
        if (management == null) {
            throw new IllegalArgumentException("The file management cannot be null.");
        }

        this.client = client;
        this.management = management;
        this.urlFileDownloader = urlFileDownloader;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * This is the main method that uses the passed MqttClient to subscribe to all the topics
     * that need to be subscribed to by the protocol.
     */
    public void subscribe() {
        try {
            // File transfer subscriptions
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_UPLOAD_INITIATE + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_UPLOAD_INITIATE, QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferInitiation(topic, message)));
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_UPLOAD_ABORT + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_UPLOAD_ABORT, QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferAbort(topic, message)));
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_BINARY_RESPONSE + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_BINARY_RESPONSE, QOS,
                    (topic, message) -> executor.execute(() -> handleFileTransferBinaryResponse(topic, message)));
            // File URL download subscriptions
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_INITIATE + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_INITIATE, QOS,
                    (topic, message) -> executor.execute(() -> handleUrlDownloadInitiation(topic, message)));
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_ABORT + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_ABORT, QOS,
                    (topic, message) -> executor.execute(() -> handleUrlDownloadAbort(topic, message)));
            // File deletion subscriptions
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_DELETE + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_DELETE, QOS,
                    (topic, message) -> executor.execute(() -> handleFileDeletion(topic, message)));
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_PURGE + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_PURGE, QOS,
                    (topic, message) -> executor.execute(() -> handleFilePurge(topic, message)));
            // File list subscriptions
            LOG.debug("Subscribing to topic '" + IN_DIRECTION + client.getClientId() + FILE_LIST + "'.");
            client.subscribe(IN_DIRECTION + client.getClientId() + FILE_LIST, QOS,
                    (topic, message) -> executor.execute(() -> handleFileListRequest(topic, message)));
        } catch (MqttException exception) {
            LOG.error(exception.getMessage());
        }
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    void handleFileTransferInitiation(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);

        // If a session is already running, that means the initialization message is not acceptable now.
        if (isSessionRunning()) {
            LOG.warn("File transfer session is already ongoing. Ignoring this message...");
            return;
        }

        // Parse the initialization message
        FileInit initMessage = JsonUtil.deserialize(message, FileInit.class);
        LOG.info("Received file transfer session, with file named '" + initMessage.getFileName() + "'.");

        // If there was an error creating the management, report a `FILE_SYSTEM_ERROR`.
        if (this.management == null) {
            LOG.error("File management is not running, returning '" + FileTransferError.FILE_SYSTEM_ERROR + "'.");
            publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, new FileStatus(initMessage.getFileName(),
                    FileTransferStatus.ERROR, FileTransferError.FILE_SYSTEM_ERROR));
            return;
        }

        // Check if the file already exists
        try {
            Boolean matching = findAndCheckFileHash(initMessage.getFileName(), initMessage.getFileHash());
            if (matching != null) {
                if (matching) {
                    LOG.info("File '" + initMessage.getFileName() + "' hashes match, returning 'FILE_READY'.");
                    publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, new FileStatus(initMessage.getFileName(),
                            FileTransferStatus.FILE_READY));
                } else {
                    LOG.info("File '" +
                            initMessage.getFileName() + "' hashes do not match, returning 'FILE_HASH_MISMATCH'.");
                    publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, new FileStatus(initMessage.getFileName(),
                            FileTransferStatus.ERROR, FileTransferError.FILE_HASH_MISMATCH));
                }
                return;
            }
        } catch (IOException exception) {
            LOG.error("Error occurred during reading of the existing file, returning '" +
                    FileTransferError.FILE_SYSTEM_ERROR + "'.");
            publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, new FileStatus(initMessage.getFileName(),
                    FileTransferStatus.ERROR, FileTransferError.FILE_SYSTEM_ERROR));
            return;
        }

        // Start the session
        fileDownloadSession = new FileDownloadSession(initMessage, new FileDownloadSession.Callback() {
            @Override
            public void sendRequest(String fileName, int chunkIndex) {
                handleFileTransferRequest(fileName, chunkIndex);
            }

            @Override
            public void onFinish(FileTransferStatus status, FileTransferError error) {
                handleFileTransferFinish(fileDownloadSession, status, error);
                fileDownloadSession = null;
            }
        }, maxChunkSize);

        // Send the transferring message
        publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, new FileStatus(initMessage.getFileName(),
                FileTransferStatus.FILE_TRANSFER));
    }

    Boolean findAndCheckFileHash(String fileName, String fileHash) throws IOException {
        File file;
        if ((file = management.getFile(fileName)) != null) {
            LOG.info("File '" + file.getName() + "' already exists.");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] existingFileHash = FileDownloadSession.calculateMD5HashForBytes(fileBytes);

            return fileHash.equalsIgnoreCase(DatatypeConverter.printHexBinary(existingFileHash));
        }
        return null;
    }

    void handleFileTransferAbort(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (fileDownloadSession == null) {
            LOG.warn("Received binary chunk data when session is not ongoing.");
            return;
        }

        // Parse the payload and check its validity
        String fileName = JsonUtil.deserialize(message, String.class);

        if (!fileName.equals(fileDownloadSession.getInitMessage().getFileName())) {
            LOG.warn("Received file transfer abort message with non-matching file name.");
            return;
        }

        // Abort the session
        LOG.info("Received request to abort file transfer. Aborting...");
        fileDownloadSession.abort();
    }

    void handleFileTransferBinaryResponse(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (fileDownloadSession == null) {
            LOG.warn("Received binary chunk data when session is not ongoing.");
            return;
        }

        try {
            // Pass on the payload
            fileDownloadSession.receiveBytes(message.getPayload());
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.error("Failed to handle bytes: " + e.getLocalizedMessage());
        }
    }

    void handleFileTransferRequest(String fileName, int chunkIndex) {
        // Create a message to request the data and send it
        ChunkRequest chunkRequest = new ChunkRequest(fileName, chunkIndex);
        publish(OUT_DIRECTION + client.getClientId() + FILE_BINARY_REQUEST, chunkRequest);
    }

    void handleFileTransferFinish(FileDownloadSession session, FileTransferStatus status,
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
            LOG.info("Reporting file transfer as '" + status + "'" +
                    (error != null ? " with error '" + error + "'" : "") + ".");
            publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS, statusMessage);
            return;
        }

        try {
            // Make the file
            management.createFile(session.getBytes(), session.getInitMessage().getFileName());

            // Announce the status for good status, and save the data from file, and publish the file list.
            publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS,
                    new FileStatus(session.getInitMessage().getFileName(), status));
            LOG.info("Reporting file transfer as successful. Downloaded file '" +
                    session.getInitMessage().getFileName() + "'.");
        } catch (IOException exception) {
            // Announce a file system error has occurred
            publish(OUT_DIRECTION + client.getClientId() + FILE_UPLOAD_STATUS,
                    new FileStatus(session.getInitMessage().getFileName(),
                            FileTransferStatus.ERROR, FileTransferError.FILE_SYSTEM_ERROR));
            LOG.info("Reporting file transfer as '" + FileTransferStatus.ERROR +
                    "' with error '" + FileTransferError.FILE_SYSTEM_ERROR + "'.");
        } finally {
            publishFileList();
        }
    }

    /**
     * This is the method that defines the behaviour when a FILE_URL_DOWNLOAD_INITIATE message is received.
     */
    void handleUrlDownloadInitiation(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // If a session is already running, that means the initialization message is not acceptable now.
        if (isSessionRunning()) {
            logReceivedMqttMessage(topic, message);
            LOG.warn("File transfer session is already ongoing. Ignoring this message...");
            return;
        }

        // Parse the initialization message
        String fileUrl = JsonUtil.deserialize(message, String.class);

        UrlInfo urlInit = new UrlInfo();
        urlInit.setFileUrl(fileUrl);

        LOG.info("Received URL file download session, with URL '" + urlInit.getFileUrl() + "'.");

        // If there is no management, return FILE_SYSTEM_ERROR immediately.
        if (this.management == null) {
            LOG.error("File management is not running, returning '" + FileTransferError.FILE_SYSTEM_ERROR + "'.");
            publish(OUT_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_STATUS, new UrlStatus(urlInit.getFileUrl(),
                    FileTransferStatus.ERROR, FileTransferError.FILE_SYSTEM_ERROR));
            return;
        }

        // Give the transfer message
        publish(OUT_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_STATUS,
                new UrlStatus(urlInit.getFileUrl(), FileTransferStatus.FILE_TRANSFER));

        // Create the session
        if (this.urlFileDownloader == null) {
            urlFileDownloadSession = new UrlFileDownloadSession(urlInit, (status, fileName, error) -> {
                handleUrlSessionFinish(urlFileDownloadSession, status, fileName, error);
                urlFileDownloadSession = null;
            });
        } else {
            urlFileDownloadSession = new UrlFileDownloadSession(urlInit, (status, fileName, error) -> {
                handleUrlSessionFinish(urlFileDownloadSession, status, fileName, error);
                urlFileDownloadSession = null;
            }, urlFileDownloader);
        }
    }

    /**
     * This is the method that defines the behaviour when a FILE_URL_DOWNLOAD_ABORT message is received.
     */
    void handleUrlDownloadAbort(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        // Null check the session
        if (urlFileDownloadSession == null) {
            LOG.warn("Received URL download abort while session is not running.");
            return;
        }

        // Parse the payload, and check its validity
        String fileUrl = JsonUtil.deserialize(message, String.class);
        if (!fileUrl.equals(urlFileDownloadSession.getInitMessage().getFileUrl())) {
            LOG.warn("Received URL download abort for non-matching URL paths.");
            return;
        }

        // Abort the message
        LOG.info("Received request to abort URL file download. Aborting...");
        urlFileDownloadSession.abort();
    }

    /**
     * This is the callback method for the URL Download session to handle the result of the session.
     */
    void handleUrlSessionFinish(UrlFileDownloadSession session, FileTransferStatus status,
                                String fileName, FileTransferError error) {
        // Null check what needs to be null checked
        if (session == null) {
            throw new IllegalStateException("Handle URL session finish is called with a null session.");
        }
        if (status == null) {
            throw new IllegalStateException("Handle URL session finish is called with a null status.");
        }

        // Announce the not good status
        if (status != FileTransferStatus.FILE_READY) {
            LOG.info("Reporting URL file download as '" + status + "'" +
                    (error != null ? " with error '" + error + "'" : "") + ".");
            UrlStatus statusMessage = new UrlStatus(session.getInitMessage().getFileUrl(), status, fileName, error);
            publish(OUT_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_STATUS, statusMessage);
            return;
        }

        try {
            // Make the file
            management.createFile(session.getFileData(), session.getFileName());

            // Announce the status for good status, and save the data from file, and publish the file list now.
            UrlStatus statusMessage = new UrlStatus(session.getInitMessage().getFileUrl(), FileTransferStatus.FILE_READY);
            publish(OUT_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_STATUS, statusMessage);
            LOG.info("Reporting URL file download as successful. Downloaded file '" + session.getFileName() + "'.");
        } catch (IOException exception) {
            // Announce a file system error has occurred
            publish(OUT_DIRECTION + client.getClientId() + FILE_URL_DOWNLOAD_STATUS,
                    new FileStatus(session.getInitMessage().getFileUrl(),
                            FileTransferStatus.ERROR, FileTransferError.FILE_SYSTEM_ERROR));
            LOG.info("Reporting URL file download as '" + FileTransferStatus.ERROR +
                    "' with error '" + FileTransferError.FILE_SYSTEM_ERROR + "'.");
        } finally {
            publishFileList();
        }
    }

    /**
     * This is the method that defines the behaviour when a FILE_DELETE message is received.
     */
    void handleFileDeletion(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);

        if (this.management == null) {
            LOG.warn("Unable to accept file deletion message, file system management is not running.");
            return;
        }

        List<String> files = JsonUtil.deserialize(message, List.class);
        FileDelete fileDelete = new FileDelete(files);

        LOG.info("Received request to delete files '" + fileDelete.getFileNames() + "'. Deleting...");

        for (String file : fileDelete.getFileNames()) {
            management.deleteFile(file);
        }

        publishFileList();
    }

    /**
     * This is the method that defines the behaviour when a FILE_PURGE message is received.
     */
    void handleFilePurge(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);

        if (this.management == null) {
            LOG.warn("Unable to accept file purge message, file system management is not running.");
            return;
        }

        LOG.info("Received request to purge file list. Purging...");
        management.purgeDirectory();

        publishFileList();
    }

    /**
     * This is the method that defines the behaviour when a FILE_LIST_REQUEST message is received.
     */
    void handleFileListRequest(String topic, MqttMessage message) {
        logReceivedMqttMessage(topic, message);
        LOG.info("Received request for the file list. Responding...");
        publishFileList();
    }

    /**
     * This is the method that is used to capture the file list and send it.
     */
    public void publishFileList() {
        // Stored values;
        List<FileInformation> payload;

        // Acquire all the files
        try {
            payload = management.listAllFiles();
            LOG.trace("Peeked the file system to find files, found " + payload.size() + " files.");
        } catch (IOException exception) {
            LOG.error("Error occurred during reading of folder contents.", exception);
            return;
        }

        LOG.trace("Created payload to announce '" + payload + "'.");

        // Send everything
        publish(OUT_DIRECTION + client.getClientId() + FILE_LIST, payload);
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
            LOG.debug("Publishing to '" + topic + "' payload: " + new String(JsonUtil.serialize(payload), StandardCharsets.UTF_8));
            client.publish(topic, JsonUtil.serialize(payload), QOS, false);
        } catch (MqttException e) {
            final String message = "MQTT error occurred while publishing a message to topic : '" +
                    topic + "' with payload: '" + payload + "'.";
            LOG.error(message, e);
        } catch (Exception e) {
            final String message = "Could not publish message to topic: '" +
                    topic + "' with payload: '" + payload + "'.";
            LOG.error(message, e);
        }
    }
}
