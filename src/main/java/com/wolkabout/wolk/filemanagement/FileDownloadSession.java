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

import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileDownloadSession {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSession.class);

    // The constant values
    private static final int MINIMUM_PACKET_SIZE = 65;
    private static final int PREVIOUS_HASH_SIZE = 32;
    private static final int CURRENT_HASH_SIZE = 32;
    private static final int CHUNK_SIZE = 1000000;
    private static final int MAX_RETRY = 3;
    private static final int MAX_RESTART = 3;

    // The main indicators of state
    private boolean running;
    private boolean success;
    private boolean aborted;

    // The input data
    private final FileInit initMessage;

    // The collected data
    private List<Integer> chunkSizes;
    private List<Byte[]> chunks;

    private byte[] headerHash;

    // The callback for when the work is done
    private final Callback callback;

    public boolean isRunning() {
        return running;
    }

    public boolean isSuccess() {
        return success;
    }

    public FileInit getInitMessage() {
        return initMessage;
    }

    public FileDownloadSession(FileInit initMessage, Callback callback) throws IllegalArgumentException {
        if (initMessage == null) {
            throw new IllegalArgumentException("The initial message object can not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("The callback object can not be null.");
        }

        this.initMessage = initMessage;
        this.running = true;

        this.chunks = new ArrayList<>();
        this.chunkSizes = new ArrayList<>();

        this.callback = callback;

        // Calculate the chunk count, and each of their sizes
        long fullChunkDataBytes = CHUNK_SIZE - (PREVIOUS_HASH_SIZE + CURRENT_HASH_SIZE);
        long fullSizedChunks = initMessage.getFileSize() / fullChunkDataBytes;
        long leftoverSizedChunk = initMessage.getFileSize() % fullChunkDataBytes;

        // Append them all into the list
        for (int i = 0; i < fullSizedChunks; i++) {
            chunkSizes.add(CHUNK_SIZE);
        }

        if (leftoverSizedChunk > 0) {
            chunkSizes.add((int) (leftoverSizedChunk + (PREVIOUS_HASH_SIZE + CURRENT_HASH_SIZE)));
        }
    }

    public boolean receiveBytes(byte[] bytes) {
        LOG.trace("Received chunk of bytes. Size of chunk: " + bytes.length + ", current chunk count: " + chunks.size()
                + (chunks.isEmpty() ? "" : ", last chunk hash: " + Arrays.toString(chunks.get(chunks.size() - 1))));

        return true;
    }

    public boolean abort() {
        this.running = false;
        this.success = false;
        this.aborted = true;
        return true;
    }

    public FileTransferStatus getCurrentStatus() {
        /*
         * Analyze the current state of session, and decide on the transfer status
         */
        if (this.running)
            return FileTransferStatus.FILE_TRANSFER;
        if (this.aborted)
            return FileTransferStatus.ABORTED;
        if (this.success)
            return FileTransferStatus.FILE_READY;
        else
            return FileTransferStatus.ERROR;
    }

    public interface Callback {
        void sendRequest(String fileName, int chunkIndex, int chunkSize);

        void onFinish(FileTransferStatus status, FileTransferError error);
    }
}
