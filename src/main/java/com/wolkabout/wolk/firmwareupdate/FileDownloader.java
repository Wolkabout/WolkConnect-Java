package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.firmwareupdate.model.ChunkRequest;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareStatus;
import com.wolkabout.wolk.firmwareupdate.model.UpdateError;
import com.wolkabout.wolk.firmwareupdate.model.command.FileInfo;
import com.wolkabout.wolk.util.JsonUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FileDownloader {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloader.class);

    private static final int MINIMUM_PACKET_SIZE = 65;
    private static final int CHUNK_SIZE = 1024;
    private static final int MAX_RETRY = 3;
    private static final int MAX_RESTART = 3;

    private final MqttClient client;
    private final Callback callback;

    private long expectedChunkCount;

    private int currentAttempt = 0;
    private FileInfo fileInfo;

    //Should be cleaned on restart
    private boolean aborted = false;
    private final List<byte[]> chunks = new ArrayList<>();
    private byte[] lastChunkHash = null;
    private int currentChunk = 0;
    private int currentRetry = 0;

    FileDownloader(MqttClient client, Callback callback) {
        this.client = client;
        this.callback = callback;
        subscribe();
    }

    public void download(FileInfo fileInfo) {
        reset();

        this.fileInfo = fileInfo;
        expectedChunkCount = fileInfo.getFileSize() / CHUNK_SIZE + 1;

        requestChunk(0);
        this.callback.onStatusUpdate(FirmwareStatus.FILE_TRANSFER);
    }

    public void abort() {
        aborted = true;
        callback.onStatusUpdate(FirmwareStatus.ABORTED);
    }

    private void subscribe() {
        try {
            client.subscribe("service/binary/" + client.getClientId(), new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (aborted || fileInfo == null) {
                        return;
                    }

                    processChunk(message.getPayload());

                    if (currentChunk == expectedChunkCount) {
                        final byte[] allBytes = aggregateFile();
                        final byte[] actualHash = DigestUtils.sha256(allBytes);
                        final byte[] expectedHash = Base64.decodeBase64(fileInfo.getFileHash());
                        if (Arrays.equals(expectedHash, actualHash)) {
                            callback.onFileReceived(allBytes);
                            callback.onStatusUpdate(FirmwareStatus.FILE_READY);
                        } else {
                            restart();
                        }
                    } else {
                        currentChunk++;
                        requestChunk(currentChunk);
                    }
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }

    private void processChunk(byte[] chunk) {
        if (chunk.length < MINIMUM_PACKET_SIZE) {
            retryLastChunk();
        }

        final byte[] providedLastChunkHash = Arrays.copyOfRange(chunk, 0, 32);
        final byte[] data = Arrays.copyOfRange(chunk, 32, chunk.length - 32);
        final byte[] providedHash = Arrays.copyOfRange(chunk, chunk.length - 32, chunk.length);

        if (currentChunk != expectedChunkCount && data.length != CHUNK_SIZE) {
            restart();
        }

        if (Arrays.equals(lastChunkHash, providedLastChunkHash)) {
            retryLastChunk();
        }

        final byte[] calculatedHash = DigestUtils.sha256(data);
        if (Arrays.equals(calculatedHash, providedHash)) {
            retryLastChunk();
        }

        this.lastChunkHash = providedHash;
        chunks.add(data);

        currentRetry = 0;
    }

    private void retryLastChunk() {
        currentRetry++;

        if (currentRetry > MAX_RETRY) {
            callback.onError(UpdateError.RETRY_COUNT_EXCEEDED);
        }

        requestChunk(currentChunk);
    }

    private void requestChunk(int index) {
        final ChunkRequest chunkRequest = new ChunkRequest();
        chunkRequest.setChunkIndex(index);
        chunkRequest.setChunkSize(CHUNK_SIZE);
        chunkRequest.setFileName(fileInfo.getFileName());

        publish("service/status/file/" + client.getClientId(), chunkRequest);
    }

    private void publish(String topic, Object payload) {
        try {
            LOG.trace("Publishing to \'" + topic + "\' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), 2, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    private void restart() {
        currentAttempt++;
        if (currentAttempt > MAX_RESTART) {
            callback.onError(UpdateError.RETRY_COUNT_EXCEEDED);
        }

        reset();
        requestChunk(0);
    }

    private void reset() {
        aborted = false;
        chunks.clear();
        lastChunkHash = null;
        currentChunk = 0;
        currentRetry = 0;
    }

    private byte[] aggregateFile() {
        int size = 0;
        for (byte[] chunk : chunks) {
            size += chunk.length;
        }

        final byte[] file = new byte[size];
        int position = 0;

        for (byte[] chunk : chunks) {
            for (byte aByte : chunk) {
                file[position] = aByte;
                position++;
            }
        }

        return file;
    }

    public interface Callback {
        void onStatusUpdate(FirmwareStatus status);
        void onError(UpdateError error);
        void onFileReceived(byte[] bytes);
    }
}
