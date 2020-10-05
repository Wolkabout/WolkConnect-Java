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
    private List<Byte[]> chunks;
    private byte[] headerHash;

    private long byteCount;

    // The callback for when the work is done
    private Callback callback;

    public boolean isRunning() {
        return running;
    }

    public boolean isSuccess() {
        return success;
    }

    public FileInit getInitMessage() {
        return initMessage;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public FileDownloadSession(FileInit initMessage) {
        this.initMessage = initMessage;
        this.running = true;

        this.chunks = new ArrayList<>();
    }

    public FileDownloadSession(FileInit initMessage, Callback callback) {
        this.initMessage = initMessage;
        this.running = true;

        this.chunks = new ArrayList<>();
        this.callback = callback;
    }

    public boolean receiveBytes(byte[] bytes) {
        LOG.trace("Received chunk of bytes. Size of chunk: " + bytes.length + ", current chunk count: " +
                chunks.size() + ", current bytes count: " + byteCount +
                (chunks.isEmpty() ? "" : ", last chunk hash: " + Arrays.toString(chunks.get(chunks.size() - 1))));

        if (bytes.length < MINIMUM_PACKET_SIZE) {

        }

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
