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

import com.wolkabout.wolk.filemanagement.model.device2platform.ChunkRequest;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import com.wolkabout.wolk.util.JsonUtil;
import jdk.internal.net.http.common.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);

    // File binary data request/response topics
    private static final String FILE_BINARY_REQUEST = "d2p/file_binary_request/d/";
    private static final String FILE_BINARY_RESPONSE = "p2d/file_binary_response/d/";

    // Constant values
    protected static final int QOS = 2;

    // MqttClient connection and external callback interface
    private MqttClient client;
    private Callback callback;

    // Transfer session data
    private FileDownloadSession session;
    private Queue<FileInit> initMessages;

    public void setClient(MqttClient client) {
        this.client = client;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String getCurrentFile() {
        if (initMessages.isEmpty()) {
            return "";
        }

        return initMessages.element().getFileName();
    }

    // Default constructor
    public FileDownloader() {
    }

    // The constructor with parameters, injected on creation or afterwards
    FileDownloader(MqttClient client, Callback callback) {
        this.client = client;
        this.callback = callback;
    }

    // The main method that prepares the MQTT message subscription
    public void prepareSubscription() throws MqttException {
        if (client == null) {
            throw new IllegalStateException("MQTT client is not set.");
        }

        try {
            LOG.trace("Subscribing to topic: '" + (FILE_BINARY_RESPONSE + client.getClientId()) + "'.\n" +
                    "Handler for topic: '" + this.getClass().getMethod("receiveBinaryResponse",
                    String.class, MqttMessage.class).toString() + "'");
        } catch (NoSuchMethodException e) {
            LOG.error("Object contains no method 'receiveBinaryResponse()'.");
        }
        client.subscribe(FILE_BINARY_RESPONSE + client.getClientId(), QOS, this::receiveBinaryResponse);
    }

    public void receiveBinaryResponse(String topic, MqttMessage message) {
        try {
            /*
             * Check prerequisites, a file init must be in the queue, and the device downloader
             * transferring status must be set to true. And the topic must be the exact one as subscribed, just in case.
             */
            if (!topic.equals(FILE_BINARY_RESPONSE + client.getClientId())) {
                throw new Exception("Handler received message for wrong topic." +
                        "'" + (FILE_BINARY_RESPONSE + client.getClientId()) + "' != '" + topic + "'.");
            }

            if (session == null || !session.isRunning())
                throw new Exception("Received '" + topic + "' message, while downloader is not transferring.");

            if (initMessages.isEmpty())
                throw new Exception("Received '" + topic + "' message, but no initialize messages are active.");

            final boolean success = processChunk(message.getPayload());
            if (!success) {
                return;
            }

//            if (currentChunk != expectedChunkCount) {
//                currentChunk++;
//                requestChunk(currentChunk);
//            } else {
//                final byte[] allBytes = aggregateFile();
//                final byte[] actualHash = DigestUtils.sha256(allBytes);
//                final byte[] expectedHash = Base64.decodeBase64(fileInit.getFileHash());
//                if (Arrays.equals(expectedHash, actualHash)) {
//                    callback.onStatusUpdate(FileTransferStatus.FILE_READY);
//                    callback.onFileReceived(fileInit.getFileName(), allBytes);
//                } else {
//                    restart();
//                }
//            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    public void download(FileInit fileInit) {
        reset();

//        this.fileInit = fileInit;
//        expectedChunkCount = fileInit.getFileSize() / CHUNK_SIZE;

        requestChunk(0);
        this.callback.onStatusUpdate(FileTransferStatus.FILE_TRANSFER);
    }

    public void abort() {
//        aborted = true;
        callback.onStatusUpdate(FileTransferStatus.ABORTED);
    }

    private boolean processChunk(byte[] chunk) {
//        if (chunk.length < MINIMUM_PACKET_SIZE) {
//            LOG.trace("Chunk size is bellow minimum. Retrying...");
//            retryLastChunk();
//            return false;
//        }

        final byte[] providedLastChunkHash = Arrays.copyOfRange(chunk, 0, 32);
        final byte[] data = Arrays.copyOfRange(chunk, 32, chunk.length - 32);
        final byte[] providedHash = Arrays.copyOfRange(chunk, chunk.length - 32, chunk.length);

//        if (currentChunk != expectedChunkCount && data.length != CHUNK_SIZE - 64) {
//            LOG.trace("Bad chunk size. Retrying..." + data.length);
//            retryLastChunk();
//            return false;
//        }
//
//        if (lastChunkHash != null && !Arrays.equals(lastChunkHash, providedLastChunkHash)) {
//            LOG.trace("Bad last chunk hash. Retrying...");
//            retryLastChunk();
//            return false;
//        }

        final byte[] calculatedHash = DigestUtils.sha256(data);
        if (!Arrays.equals(calculatedHash, calculatedHash)) {
            LOG.trace("Bad current chunk hash. Retrying...");
            retryLastChunk();
            return false;
        }

//        this.lastChunkHash = providedHash;
//        chunks.add(data);
//
//        LOG.trace("Received chunk: " + (currentChunk + 1) + "/" + (expectedChunkCount + 1));
//        currentRetry = 0;
        return true;
    }

    private void retryLastChunk() {
//        currentRetry++;
//
//        if (currentRetry > MAX_RETRY) {
//            callback.onError(FileTransferError.FILE_SYSTEM_ERROR);
//            return;
//        }
//
//        requestChunk(currentChunk);
    }

    private void requestChunk(int index) {
//        final ChunkRequest chunkRequest = new ChunkRequest(fileInit.getFileName(), index, CHUNK_SIZE);
//        publish("service/status/file/" + client.getClientId(), chunkRequest);
    }

    private void publish(String topic, Object payload) {
        try {
            LOG.trace("Publishing to '" + topic + "' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), QOS, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    private void restart() {
//        currentAttempt++;
//        if (currentAttempt > MAX_RESTART) {
//            callback.onError(FileTransferError.FILE_SYSTEM_ERROR);
//        }

        reset();
        requestChunk(0);
    }

    private void reset() {
//        aborted = false;
//        chunks.clear();
//        lastChunkHash = null;
//        currentChunk = 0;
//        currentRetry = 0;
    }

    private byte[] aggregateFile() {
        int size = 0;
//        for (byte[] chunk : chunks) {
//            size += chunk.length;
//        }

        final byte[] file = new byte[size];
//        int position = 0;
//
//        for (byte[] chunk : chunks) {
//            for (byte aByte : chunk) {
//                file[position] = aByte;
//                position++;
//            }
//        }
//
//        chunks.clear();
        return file;
    }

    // The interface containing all methods used by the downloader to notify the outside.
    public interface Callback {
        void onStatusUpdate(FileTransferStatus status);

        void onError(FileTransferError error);

        void onFileReceived(String fileName, byte[] bytes);
    }
}
