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
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a class that represents a single file transfer session.
 * Session starts with the initiate message from platform, and then we request all the chunks
 * until we finally assembled all the bytes into a single place, where the file is compiled.
 * Checks of hashes are done in between, when we can re-request anything in between of runtime.
 */
public class FileDownloadSession {

    // The Logger
    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSession.class);

    // The constant values
    private static final int MINIMUM_PACKET_SIZE = 65;
    private static final int PREVIOUS_HASH_SIZE = 32;
    private static final int CURRENT_HASH_SIZE = 32;
    private static final int CHUNK_SIZE = 250000;
    private static final int MAX_RETRY = 3;
    private static final int MAX_RESTART = 3;
    // The executor
    private final ExecutorService executor = Executors.newCachedThreadPool();
    // The input data
    private final FileInit initMessage;
    private final Callback callback;
    // The collected data
    private final List<Integer> chunkSizes;
    private final List<Byte> bytes;
    private final List<byte[]> hashes;
    // The main indicators of state
    private int currentChunk;
    private int chunkRetryCount;
    private int restartCount;
    // The end status variables
    private FileTransferStatus status;
    private FileTransferError error;

    /**
     * The default constructor for the class. Bases the download session off the passed message data about the file
     * that needs to be received, and contains external calls to request more incoming data, and notify of the finished
     * status.
     *
     * @param initMessage The parsed message object that contains information about a file that needs to be transferred.
     * @param callback    The object containing external calls for requesting data and notifying of finish.
     * @throws IllegalArgumentException If any of the arguments is given null, the exception will be thrown.
     */
    public FileDownloadSession(FileInit initMessage, Callback callback) throws IllegalArgumentException {
        if (initMessage == null) {
            throw new IllegalArgumentException("The initial message object can not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("The callback object can not be null.");
        }

        this.initMessage = initMessage;
        this.callback = callback;

        this.bytes = new ArrayList<>();
        this.hashes = new ArrayList<>();
        this.chunkSizes = new ArrayList<>();

        // Calculate the chunk count, and each of their sizes
        long fullSizedChunks = initMessage.getFileSize() / CHUNK_SIZE;
        long leftoverSizedChunk = initMessage.getFileSize() % CHUNK_SIZE;

        // Append them all into the list
        for (int i = 0; i < fullSizedChunks; i++) {
            chunkSizes.add(CHUNK_SIZE + (PREVIOUS_HASH_SIZE + CURRENT_HASH_SIZE));
        }

        if (leftoverSizedChunk > 0) {
            chunkSizes.add((int) (leftoverSizedChunk + (PREVIOUS_HASH_SIZE + CURRENT_HASH_SIZE)));
        }
        LOG.trace("Calculated chunk count for this file: " + chunkSizes.size());

        // Request the first chunk
        status = FileTransferStatus.FILE_TRANSFER;
        error = null;
        LOG.trace("Requesting first chunk of data.");
        executor.execute(new RequestRunnable(initMessage.getFileName(), currentChunk, chunkSizes.get(currentChunk)));
    }

    /**
     * This is an internal method used to define how a chunk of bytes is hashed.
     *
     * @param data Input bytes to be calculated a SHA256 hash from.
     * @return The SHA256 hash of input data as byte array.
     */
    public static byte[] calculateHashForBytes(List<Byte> data) {
        byte[] bytes = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            bytes[i] = data.get(i);
        }
        return DigestUtils.sha256(bytes);
    }

    /**
     * This is an internal method used to define how a chunk of bytes is hashed.
     *
     * @param data Input bytes to be calculated a SHA256 hash from.
     * @return The SHA256 hash of input data as byte array.
     */
    public static byte[] calculateHashForBytes(byte[] data) {
        return DigestUtils.sha256(data);
    }

    public FileInit getInitMessage() {
        return initMessage;
    }

    public FileTransferStatus getStatus() {
        return status;
    }

    public FileTransferError getError() {
        return error;
    }

    public byte[] getBytes() {
        byte[] newBytes = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            newBytes[i] = bytes.get(i);
        }
        return newBytes;
    }

    /**
     * This is the method used to receive the external message that this file transfer needs to be aborted.
     * This will set the state of the session to aborted, notify the external of this state,
     * and stop receiving any data, and fold everything.
     *
     * @return Whether the session was successfully aborted. If it wasn't running, it will not be aborted. If it was
     * successful, it will not be aborted, and if it had already thrown an error, it will not be aborted.
     */
    public synchronized boolean abort() {
        switch (this.status) {
            case FILE_READY:
                LOG.warn("Unable to abort transfer session. Session is done and successful.");
                return false;
            case ABORTED:
                LOG.warn("Unable to abort transfer session. Session is already aborted.");
                return false;
            case ERROR:
                LOG.warn("Unable to abort transfer session. Session is done with error '" + error.toString() + "'.");
                return false;
            case FILE_TRANSFER:
                currentChunk = 0;
                bytes.clear();

                status = FileTransferStatus.ABORTED;
                error = null;
                executor.execute(new FinishRunnable(status, null));

                return true;
            default:
                return false;
        }
    }

    /**
     * This is the method used to receive data from the response after this session sent a request for a specific chunk.
     * In here, the payload is analyzed to verify that it is valid, and if it is not, to retry to obtain a chunk,
     * or reset the entire process if necessary.
     *
     * @param receivedBytes Entire payload from response, containing 32 bytes for previous chunk hash,
     *                      bytes from current chunk, and additional 32 bytes for the current chunk hash.
     * @return Whether this specific chunk processing was successful. The session will itself request the chunk again if
     * it decides that is something it wants to do, because it may be over the limits, and will want to report
     * an error, since it had retried too many times.
     */
    public synchronized boolean receiveBytes(byte[] receivedBytes) {
        LOG.trace("Received chunk of bytes. Size of chunk: " + receivedBytes.length + ", " +
                "current chunk count: " + currentChunk + ".");

        // Check the session status
        if (this.status != FileTransferStatus.FILE_TRANSFER)
            throw new IllegalStateException("This session is not running anymore, it is not accepting chunks.");

        // Check the array size
        if (receivedBytes.length < MINIMUM_PACKET_SIZE)
            throw new IllegalArgumentException("The passed bytes is not a valid chunk message.");
        if (receivedBytes.length != chunkSizes.get(currentChunk))
            throw new IllegalArgumentException("The passed bytes is not the same size as requested.");

        // Obtain the previous hash
        byte[] previousHash = Arrays.copyOfRange(receivedBytes, 0, 32);
        byte[] chunkData = Arrays.copyOfRange(receivedBytes, 32, receivedBytes.length - 32);
        byte[] currentHash = Arrays.copyOfRange(receivedBytes, receivedBytes.length - 32, receivedBytes.length);

        // Analyze the chunk received.
        if (currentChunk == 0) {
            // Analyze the first hash to be all zeroes.
            if (!Arrays.equals(previousHash, new byte[32])) {
                LOG.warn("Invalid header for first chunk, previous hash is not 0.");
                return requestChunkAgain(initMessage.getFileName(), currentChunk, chunkSizes.get(currentChunk));
            }
        } else {
            // Analyze the hash of last chunk with the hash received in this message for chunk before.
            if (!Arrays.equals(previousHash, hashes.get(hashes.size() - 1))) {
                // Return a chunk back, remove the hash, and delete the bytes
                LOG.warn("Received hash for previous chunk and calculated hash of previous chunk do not match.");
                --currentChunk;
                hashes.remove(currentChunk);
                for (int i = 0; i < chunkSizes.get(currentChunk); i++) {
                    if (bytes.isEmpty())
                        break;
                    bytes.remove(bytes.size() - 1);
                }
                return requestChunkAgain(initMessage.getFileName(), currentChunk, chunkSizes.get(currentChunk));
            }
        }

        // Calculate the hash for current data and check it
        byte[] calculatedHash = calculateHashForBytes(chunkData);
        if (!Arrays.equals(calculatedHash, currentHash)) {
            LOG.warn("Hash of the current chunk calculated does not match the sent hash.");
            return requestChunkAgain(initMessage.getFileName(), currentChunk, chunkSizes.get(currentChunk));
        }

        // Append all the chunk data into the bytes
        for (byte chunkByte : chunkData) {
            bytes.add(chunkByte);
        }
        // Append the hash
        hashes.add(currentHash);

        // Check if the file is fully here now.
        if (++currentChunk == chunkSizes.size() && initMessage.getFileSize() == bytes.size()) {
            // If the entire file hash is invalid, restart the entire process
            if (!Arrays.equals(calculateHashForBytes(bytes), Base64.decodeBase64(initMessage.getFileHash()))) {
                return restartDataObtain();
            }

            // Return everything
            status = FileTransferStatus.FILE_READY;
            error = null;
            executor.execute(new FinishRunnable(status, null));
            return true;
        }

        // Request the next chunk
        if (chunkSizes.size() > 1) {
            executor.execute(new RequestRunnable(initMessage.getFileName(), currentChunk,
                    CHUNK_SIZE + PREVIOUS_HASH_SIZE + CURRENT_HASH_SIZE));
        } else {
            executor.execute(new RequestRunnable(initMessage.getFileName(), currentChunk,
                    chunkSizes.get(currentChunk)));
        }
        return true;
    }

    /**
     * This is an internal method used to define how a chunk for
     * which the current hash is invalid, will be re-obtained.
     */
    private boolean requestChunkAgain(String fileName, int chunkIndex, int chunkSize) {
        LOG.debug("Requesting a chunk again(" + chunkIndex + ").");

        // If we already requested the chunk over the limit, restart the process
        if (chunkRetryCount == MAX_RETRY) {
            announceMaxRetry();
            return false;
        }

        // Increment the counter, and request the chunk again
        ++chunkRetryCount;
        executor.execute(new RequestRunnable(fileName, chunkIndex, chunkSize));
        return true;
    }

    /**
     * This is an internal method that defines the behaviour when the retries have been
     * used up, and the session should be folded and should return the appropriate error.
     */
    private void announceMaxRetry() {
        LOG.warn("A single chunk has been re-requested " + chunkRetryCount +
                " times, achieving the limit. Restarting the process.");
        currentChunk = 0;
        bytes.clear();
        chunkSizes.clear();
        hashes.clear();

        status = FileTransferStatus.ERROR;
        error = FileTransferError.RETRY_COUNT_EXCEEDED;

        executor.execute(new FinishRunnable(status, error));
    }

    /**
     * This is an internal method used to define how the entire session will be restarted
     * after the chunk reacquire has been called to the limit.
     */
    private boolean restartDataObtain() {
        LOG.debug("Restarting the data obtain session.");

        // If we already restarted the file obtain too much times, set the state to error and notify
        if (restartCount == MAX_RESTART) {
            announceMaxRestart();
            return false;
        }

        // Increment the counter, restart all the data
        ++restartCount;
        chunkRetryCount = 0;
        currentChunk = 0;
        bytes.clear();
        hashes.clear();

        // Request the first chunk again
        LOG.debug("Requesting first chunk after restart.");
        executor.execute(new RequestRunnable(initMessage.getFileName(), 0, chunkSizes.get(0)));
        return true;
    }

    /**
     * This is an internal method that defines the behaviour when the all restarts have been
     * used up, and the session should be folded and should return the appropriate error.
     */
    private void announceMaxRestart() {
        LOG.warn("The session was restarted " + restartCount +
                " times, achieving the limit. Returning error.");
        currentChunk = 0;
        bytes.clear();
        chunkSizes.clear();
        hashes.clear();

        status = FileTransferStatus.ERROR;
        error = FileTransferError.RETRY_COUNT_EXCEEDED;

        executor.execute(new FinishRunnable(status, error));
    }

    /**
     * This is the public Callback interface for this class. It contains two calls, sendRequest that should be routed
     * to send a message requesting the specified chunk of data, and onFinish that returns the result of work from this
     * session.
     */
    public interface Callback {
        void sendRequest(String fileName, int chunkIndex, int chunkSize);

        void onFinish(FileTransferStatus status, FileTransferError error);
    }

    /**
     * This is a private class that represents how a Runnable for calling `sendRequest` is supposed to look like.
     */
    private class RequestRunnable implements Runnable {

        private final String fileName;
        private final int currentChunk;
        private final int chunkSize;

        public RequestRunnable(String fileName, int currentChunk, int chunkSize) {
            this.fileName = fileName;
            this.currentChunk = currentChunk;
            this.chunkSize = chunkSize;
        }

        @Override
        public void run() {
            callback.sendRequest(fileName, currentChunk, chunkSize);
        }
    }

    /**
     * This is a private class that represents how a Runnable for calling `onFinish` is supposed to look like.
     */
    private class FinishRunnable implements Runnable {

        private final FileTransferStatus status;
        private final FileTransferError error;

        public FinishRunnable(FileTransferStatus status, FileTransferError error) {
            this.status = status;
            this.error = error;
        }

        @Override
        public void run() {
            callback.onFinish(status, error);
        }
    }
}
