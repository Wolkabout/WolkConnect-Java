/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private static final int DEFAULT_DOWNLOAD_CHUNK_SIZE = 16384;

    // The executor
    private final ExecutorService executor = Executors.newCachedThreadPool();
    // The input data
    private final UrlInfo initMessage;
    private final Callback callback;
    private Future<?> downloadTask;
    private final UrlFileDownloader urlFileDownloader;
    // The end result data
    private byte[] fileData;
    private String fileName = "";
    // The end status variables
    private FileTransferStatus status;
    private FileTransferError error;

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

        this.fileData = new byte[0];

        this.urlFileDownloader = this::defaultDownloadFile;

        // Start the download
        status = FileTransferStatus.FILE_TRANSFER;
        error = null;
        downloadTask = executor.submit(new DownloadRunnable(initMessage.getFileUrl()));
    }

    /**
     * The constructor for the class that allows custom URL download logic.
     * Bases the download session off the passed message data
     * which contains a single URL we need to target as a GET HTTP request to obtain the file, and place
     * all the acquired bytes into a local file.
     *
     * @param initMessage       The parsed message object that contains the url.
     * @param callback          The object containing external calls for notifying of finish.
     * @param urlFileDownloader The implementation of the interface that allows custom URL download logic.
     */
    public UrlFileDownloadSession(UrlInfo initMessage, Callback callback, UrlFileDownloader urlFileDownloader) {
        if (initMessage == null) {
            throw new IllegalArgumentException("The initial message object can not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("The callback object can not be null.");
        }

        this.initMessage = initMessage;
        this.callback = callback;

        this.fileData = new byte[0];

        this.urlFileDownloader = urlFileDownloader;

        // Start the download
        status = FileTransferStatus.FILE_TRANSFER;
        error = null;
        downloadTask = executor.submit(new DownloadRunnable(initMessage.getFileUrl()));
    }

    public UrlInfo getInitMessage() {
        return initMessage;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
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
        switch (this.status) {
            case FILE_READY:
                LOG.warn("Unable to abort transfer session. Session is done and successful.");
                return false;
            case ABORTED:
                LOG.warn("Unable to abort transfer session. Session is already aborted.");
                return false;
            case ERROR:
                LOG.warn("Unable to abort transfer session. Session is not running.");
                return false;
            case FILE_TRANSFER:
                // Stop the task
                if (!downloadTask.isCancelled() && !downloadTask.isDone()) {
                    downloadTask.cancel(true);
                }
                this.fileData = new byte[0];
                this.fileName = "";

                // Set the state for aborted
                this.status = FileTransferStatus.ABORTED;
                error = null;

                // Call the callback
                executor.execute(new UrlFileDownloadSession.FinishRunnable(status, fileName, null));

                return true;
            default:
                return false;
        }
    }

    /**
     * This is the method used to obtain the bytes by sending a GET HTTP request to the URL passed as argument.
     *
     * @param url HTTP path showing directly to the location that will return a file.
     * @return Success of the operation. The method will call the callback event `onFinish`, with the results, and if it
     * is successful, the data will be set into variables inside the object, obtainable through getters.
     */
    public synchronized boolean downloadFile(String url) {
        // Obtain the status and do the operation
        UrlFileDownloadResult result = urlFileDownloader.downloadFile(url);
        if (status == FileTransferStatus.ABORTED) {
            fileData = new byte[0];
            fileName = "";
            return false;
        }
        // Store the result
        status = result.getStatus();
        error = result.getError();
        fileName = result.getFileName();
        // Call the returns with appropriate values
        executor.execute(new FinishRunnable(status, fileName, error));
        return status == FileTransferStatus.FILE_READY;
    }

    public UrlFileDownloadResult defaultDownloadFile(String fileUrl) {
        try {
            final URL remoteFile = new URL(fileUrl);
            final InputStream inputStream = remoteFile.openStream();
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            byte[] data = new byte[DEFAULT_DOWNLOAD_CHUNK_SIZE];
            int read;
            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            buffer.flush();

            final String[] urlParts = fileUrl.split("/");
            String fileName = urlParts[urlParts.length - 1];
            fileData = buffer.toByteArray();

            return new UrlFileDownloadResult(FileTransferStatus.FILE_READY, fileName);
        } catch (MalformedURLException exception) {
            return new UrlFileDownloadResult(FileTransferError.MALFORMED_URL);
        } catch (Exception exception) {
            return new UrlFileDownloadResult(FileTransferError.UNKNOWN);
        }
    }

    /**
     * This is the public Callback interface for this class. Only call here needed is for
     * announcing the external of status.
     */
    public interface Callback {
        void onFinish(FileTransferStatus status, String fileName, FileTransferError error);
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
            if (downloadFile(url)) {
                LOG.info("File download on url '" + this.url + "' was successful.");
            } else {
                LOG.info("File download on url '" + this.url + "' was not successful.");
            }
        }
    }

    /**
     * This is a private class that represents how a Runnable for calling `onFinish` is supposed to look like.
     */
    private class FinishRunnable implements Runnable {

        private final FileTransferStatus status;
        private final String fileName;
        private final FileTransferError error;

        public FinishRunnable(FileTransferStatus status, String fileName, FileTransferError error) {
            this.status = status;
            this.fileName = fileName;
            this.error = error;
        }

        @Override
        public void run() {
            callback.onFinish(status, fileName, error);
        }
    }
}
