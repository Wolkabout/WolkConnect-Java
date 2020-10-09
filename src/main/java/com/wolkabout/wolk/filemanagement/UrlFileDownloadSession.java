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
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is a class that represents a single url file download session.
 * Session starts with the initiate message from platform, and then we send the HTTP request,
 * acquire the file, report back the status of how the procedure has gone.
 */
public class UrlFileDownloadSession {

    // The Logger
    private static final Logger LOG = LoggerFactory.getLogger(UrlFileDownloadSession.class);

    // The executor
    private final ExecutorService executor = Executors.newCachedThreadPool();
    // The input data
    private final UrlInfo initMessage;
    private final Callback callback;
    // The main indicators of state
    private boolean running;
    private boolean success;
    private boolean aborted;
    // The end status variables
    private FileTransferStatus status;
    private FileTransferError error;
    private Future<?> downloadTask;

    /**
     * The default constructor for the class. Bases the download session off the passed message data
     * which contains a single URL we need to target as a GET HTTP request to obtain the file, and place
     * all the acquired bytes into a local file.
     *
     * @param initMessage The parsed message object that contains the url.
     * @param callback    The object containing external calls for notifying of finish.
     */
    public UrlFileDownloadSession(UrlInfo initMessage, Callback callback) {
        if (initMessage == null) {
            throw new IllegalArgumentException("The initial message object can not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("The callback object can not be null.");
        }

        this.initMessage = initMessage;
        this.callback = callback;
        this.running = true;

        // Start the download
        status = getCurrentStatus();
        error = null;
        downloadTask = executor.submit(new DownloadRunnable(initMessage.getFileUrl()));
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isAborted() {
        return aborted;
    }

    public UrlInfo getInitMessage() {
        return initMessage;
    }

    public FileTransferStatus getStatus() {
        return status;
    }

    public FileTransferError getError() {
        return error;
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
        // If the session has thrown an error
        if (this.aborted) {
            LOG.warn("Unable to abort transfer session. Session is already aborted.");
            return false;
        }
        // If the session was successful
        if (this.success) {
            LOG.warn("Unable to abort transfer session. Session is done and successful.");
            return false;
        }
        // If the session is not running
        if (!this.running) {
            LOG.warn("Unable to abort transfer session. Session is not running.");
            return false;
        }
        // If the task is not running
        if (downloadTask == null || downloadTask.isDone()) {
            LOG.warn("The file download is already done.");
            return false;
        }

        // Stop the task
        downloadTask.cancel(true);

        // Set the state for aborted
        this.running = false;
        this.success = false;
        this.aborted = true;

        status = getCurrentStatus();
        error = null;

        // Call the callback
        executor.execute(new UrlFileDownloadSession.FinishRunnable(status, null));

        return true;
    }

    /**
     * This is the method used to obtain the bytes by sending a GET HTTP request to the URL passed as argument.
     *
     * @param url
     * @return
     */
    public synchronized boolean downloadFile(String url) {
        try {
            final URL remoteFile = new URL(url);
        } catch (MalformedURLException exception) {
            error = FileTransferError.MALFORMED_URL;
        } catch (Exception exception) {
            error = FileTransferError.UNSPECIFIED_ERROR;
        } finally {
            this.running = false;
            this.success = this.error == null;

            status = getCurrentStatus();

            executor.execute(new FinishRunnable(status, error));
        }
        return this.success;
    }

    /**
     * This is an internal method used to calculate based on the state, what is the current File Transfer Status.
     *
     * @return The transfer status described with `FileTransferStatus` enum value.
     */
    private FileTransferStatus getCurrentStatus() {
        if (this.running) {
            LOG.debug("The session status now is '" + FileTransferStatus.FILE_TRANSFER.name() + "'.");
            return FileTransferStatus.FILE_TRANSFER;
        }
        if (this.aborted) {
            LOG.debug("The session status now is '" + FileTransferStatus.ABORTED.name() + "'.");
            return FileTransferStatus.ABORTED;
        }
        if (this.success) {
            LOG.debug("The session status now is '" + FileTransferStatus.FILE_READY.name() + "'.");
            return FileTransferStatus.FILE_READY;
        }
        LOG.debug("The session status now is '" + FileTransferStatus.ERROR.name() + "'.");
        return FileTransferStatus.ERROR;
    }

    /**
     * This is the public Callback interface for this class. Only call here needed is for
     * announcing the external of status.
     */
    public interface Callback {
        void onFinish(FileTransferStatus status, FileTransferError error);
    }

    /**
     * This is a private class that represents how a Runnable will download the requested file.
     */
    private class DownloadRunnable implements Runnable {

        private final String url;

        public DownloadRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            downloadFile(url);
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
