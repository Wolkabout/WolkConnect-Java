/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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

import com.wolkabout.wolk.FirmwareDownloadHandler;
import com.wolkabout.wolk.FirmwareUpdateHandler;
import com.wolkabout.wolk.filetransfer.FileAssembler;
import com.wolkabout.wolk.filetransfer.FileReceiver;
import com.wolkabout.wolk.filetransfer.FileTransferPacket;
import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FirmwareUpdate implements FileReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(FirmwareUpdate.class);

    private static final Path FIRMWARE_VERSION_FILE = Paths.get(".dfu-version");

    private enum State {
        IDLE,
        PACKET_FILE_TRANSFER,
        URL_DOWNLOAD,
        FILE_OBTAINED,
        INSTALL
    }

    private final String firmwareVersion;

    private final FirmwareUpdateHandler firmwareUpdateHandler;
    private final FirmwareDownloadHandler firmwareDownloadHandler;

    private final long maximumFirmwareFileSize;

    private long packetTimeoutMilliseconds = 60 * 1000;
    private long abortTimePeriodMilliseconds = 5 * 1000;

    private final FileAssembler fileAssembler;

    private Listener listener;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> onPacketTimeout;

    private Path firmwareFile;
    private boolean autoInstall;

    private State state;

    public FirmwareUpdate(String firmwareVersion, Path downloadDirectory, long maximumFirmwareSize,
                          FirmwareUpdateHandler firmwareUpdateHandler, FirmwareDownloadHandler firmwareDownloadHandler) {
        this.firmwareVersion = firmwareVersion;

        this.firmwareUpdateHandler = firmwareUpdateHandler;
        this.firmwareDownloadHandler = firmwareDownloadHandler;

        this.maximumFirmwareFileSize = maximumFirmwareSize;

        this.fileAssembler = new FileAssembler(downloadDirectory, 3);
        this.fileAssembler.setListener(this);

        this.state = State.IDLE;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setPacketTimeout(long millis) {
        packetTimeoutMilliseconds = millis;
    }

    public void setAbortTimePeriod(long millis) {
        abortTimePeriodMilliseconds = millis;
    }

    public void handleCommand(FirmwareUpdateCommand command) {
        LOG.debug("Handling command: {}", command.getType());

        if (firmwareUpdateHandler == null) {
            LOG.warn("Ignoring firmware update command: {} - Reason: Firmware update disabled", command.getType());
            listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
            return;
        }

        switch (command.getType()) {
            case FILE_UPLOAD:
                handleFileUpload(command.getFileName(), command.getFileSize(),
                        command.getBase64FileSha256(), command.getAutoInstall());
                break;

            case URL_DOWNLOAD:
                if (firmwareDownloadHandler == null) {
                    LOG.warn("Ignoring firmware update command: {} - Reason: Firmware download from URL disabled", command.getType());
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
                    return;
                }

                handleUrlDownload(command.getFileUrl(), command.getAutoInstall());
                break;

            case INSTALL:
                handleInstall();
                break;

            case ABORT:
                handleAbort();
                break;

            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    public void handlePacket(FileTransferPacket packet) {
        switch (state) {
            case PACKET_FILE_TRANSFER:
                LOG.debug("Processing file transfer packet");
                onPacketTimeout.cancel(false);

                final FileAssembler.PacketProcessingError error = fileAssembler.processPacket(packet);

                if (error == FileAssembler.PacketProcessingError.RETRY_COUNT_EXCEEDED) {
                    state = State.IDLE;
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.RETRY_COUNT_EXCEEDED));
                    return;
                }

                if (error == FileAssembler.PacketProcessingError.UNABLE_TO_FINALIZE_FILE_CREATION) {
                    state = State.IDLE;
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_SYSTEM_ERROR));
                    return;
                }

                sendFilePacketRequest(fileAssembler.packetRequest());
                break;

            case IDLE:
                LOG.warn("Ignoring file transfer packet. Reason: Firmware update sequence not initialized");
                break;
            case URL_DOWNLOAD:
                LOG.warn("Ignoring file transfer packet. Reason: Firmware update sequence initiated with firmware download via URL");
                break;
            case FILE_OBTAINED:
            case INSTALL:
                LOG.warn("Ignoring file transfer packet. Reason: Firmware file transfer completed");
                break;

            default:
                throw new IllegalStateException("State: " + state);
        }
    }

    public void reportFirmwareUpdateResult() {
        if (!Files.exists(FIRMWARE_VERSION_FILE)) {
            return;
        }

        final String savedFirmwareVersion = getSavedFirmwareVersion();

        if (!firmwareVersion.equals(savedFirmwareVersion)) {
            LOG.info("Firmware update successful. Updated firmware from version '{}' to '{}'", savedFirmwareVersion, firmwareVersion);
            listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.COMPLETED));
        } else {
            LOG.error("Firmware update failed. Current firmware version {}", firmwareVersion);
            listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.INSTALLATION_FAILED));
        }

        try {
            Files.delete(FIRMWARE_VERSION_FILE);
        } catch (IOException e) {
            LOG.error("Error deleting file containing previous firmware version", e);
        }
    }

    private void handleFileUpload(String fileName, long fileSize, String fileSha256, boolean shouldAutoInstall) {
        switch (state) {
            case IDLE:
                if (maximumFirmwareFileSize == 0) {
                    LOG.error("Unable to initialize firmware update procedure. Reason: File upload is disabled");
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
                    return;
                }

                if (fileSize > maximumFirmwareFileSize) {
                    LOG.error("Unable to initialize firmware update procedure. Reason: Unsupported file size {}", fileSize);
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.UNSUPPORTED_FILE_SIZE));
                    return;
                }

                LOG.info("Firmware update parameters - File name: {}, File size: {}, Base64 SHA-256: {}", fileName, fileSize, fileSha256);
                if (!fileAssembler.initialize(fileName, fileSize, Base64.decodeBase64(fileSha256))) {
                    LOG.error("Unable to initialize firmware update procedure. Reason: File system error");
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_SYSTEM_ERROR));
                    return;
                }

                LOG.info("Initiating file transfer sequence");
                autoInstall = shouldAutoInstall;
                state = State.PACKET_FILE_TRANSFER;
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_TRANSFER));
                sendFilePacketRequest(fileAssembler.packetRequest());
                break;

            case PACKET_FILE_TRANSFER:
            case URL_DOWNLOAD:
            case FILE_OBTAINED:
            case INSTALL:
                LOG.warn("Ignoring file upload command. Reason: Firmware update already initiated");
                break;

            default:
                throw new IllegalStateException("State: " + state);
        }
    }

    private void handleUrlDownload(String file, boolean autoInstall) {
        switch (state) {
            case IDLE:
                this.autoInstall = autoInstall;
                state = State.URL_DOWNLOAD;

                try {
                    startUrlFirmwareDownload(new URL(file));
                } catch (MalformedURLException e) {
                    LOG.error("Received file URL is not valid", e);
                    state = State.IDLE;
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.MALFORMED_URL));
                }
                break;

            case PACKET_FILE_TRANSFER:
            case URL_DOWNLOAD:
            case FILE_OBTAINED:
            case INSTALL:
                LOG.warn("Ignoring url download command. Reason: Firmware update already initiated");
                break;

            default:
                throw new IllegalStateException("State: " + state);
        }
    }

    private void handleInstall() {
        switch (state) {
            case FILE_OBTAINED:
                LOG.info("Starting firmware update");
                state = State.INSTALL;
                startFirmwareUpdate();
                break;

            case INSTALL:
                LOG.warn("Ignoring install command. Reason: Installation already started");
                break;

            case IDLE:
            case PACKET_FILE_TRANSFER:
            case URL_DOWNLOAD:
                LOG.warn("Resetting to initial state and aborting on-going file transfer, if any. Reason: Illegal flow");
                state = State.IDLE;
                listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.UNSPECIFIED));
                break;

            default:
                throw new IllegalStateException("State: " + state);
        }
    }

    private void handleAbort() {
        switch (state) {
            case PACKET_FILE_TRANSFER:
                LOG.info("Aborting file transfer");
                fileAssembler.abort();
                if (onPacketTimeout != null) {
                    onPacketTimeout.cancel(false);
                }

                state = State.IDLE;
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.ABORTED));
                break;

            case URL_DOWNLOAD:
                LOG.info("Aborting file download from MQTT");
                stopUrlFirmwareDownload();

                state = State.IDLE;
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.ABORTED));
                break;

            case INSTALL:
                LOG.info("Aborting firmware update");
                stopFirmwareUpdate();
                /* Intentional fallthrough */
            case FILE_OBTAINED:
                LOG.info("Discarding obtained firmware file parts");
                if (firmwareFile != null && firmwareFile.toFile().exists()) {
                    firmwareFile.toFile().delete();
                }

                state = State.IDLE;
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.ABORTED));
                break;

            case IDLE:
                LOG.warn("Ignoring 'ABORT' command. Reason: Illegal flow");
                listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.UNSPECIFIED));
                break;

            default:
                throw new IllegalStateException("State: " + state);
        }
    }

    private void sendFilePacketRequest(FileTransferPacketRequest request) {
        if (request == null) {
            return;
        }

        executorService = Executors.newScheduledThreadPool(1);
        onPacketTimeout = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                LOG.error("Aborting firmware update. Reason: Requested file transfer packet timeout");
                fileAssembler.abort();

                state = State.IDLE;
                listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.UNSPECIFIED));
            }
        }, packetTimeoutMilliseconds, TimeUnit.MILLISECONDS);
        listenerOnFilePacketRequest(request);
    }

    private void startFirmwareUpdate() {
        LOG.info("Scheduling firmware update {} milliseconds from now", abortTimePeriodMilliseconds);
        executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.INSTALLATION));
                LOG.debug("Firmware update started");

                if (!saveFirmwareVersion()) {
                    state = State.IDLE;
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.INSTALLATION_FAILED));
                }

                firmwareUpdateHandler.updateFirmwareWithFile(firmwareFile);
            }
        }, abortTimePeriodMilliseconds, TimeUnit.MILLISECONDS);
    }

    private void stopFirmwareUpdate() {
        LOG.debug("Stopping firmware update");
        executorService.shutdownNow();
    }

    private void startUrlFirmwareDownload(final URL file) {
        LOG.info("Scheduling firmware file download from URL {} milliseconds from now", abortTimePeriodMilliseconds);
        executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_TRANSFER));
                LOG.debug("Firmware file download started");

                try {
                    final Path downloadedFile = firmwareDownloadHandler.downloadFile(file);
                    onFileReceived(downloadedFile);
                } catch (IOException e) {
                    LOG.error("Firmware file could not be downloaded", e);
                    state = State.IDLE;
                    listenerOnStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_SYSTEM_ERROR));
                }
            }
        }, abortTimePeriodMilliseconds, TimeUnit.MILLISECONDS);
    }

    private void stopUrlFirmwareDownload() {
        LOG.debug("Stopping firmware download");
        executorService.shutdownNow();
    }

    private boolean saveFirmwareVersion() {
        try {
            Files.write(FIRMWARE_VERSION_FILE, firmwareVersion.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            LOG.error("Could not save current firmware version to file");
            return false;
        }
    }

    private String getSavedFirmwareVersion() {
        try {
            return new String(Files.readAllBytes(FIRMWARE_VERSION_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Could not read saved firmware version", e);
            return "";
        }
    }

    @Override
    public void onFileReceived(Path file) {
        LOG.debug("Firmware file received: {}", file.toAbsolutePath());
        firmwareFile = file;
        state = State.FILE_OBTAINED;
        listenerOnStatus(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_READY));

        if (autoInstall) {
            LOG.debug("Commencing firmware auto install");
            state = State.INSTALL;
            startFirmwareUpdate();
        }
    }

    public void setListener(FirmwareUpdate.Listener listener) {
        this.listener = listener;
    }

    private void listenerOnStatus(FirmwareUpdateStatus status) {
        if (listener != null) {
            listener.onStatus(status);
        }
    }

    private void listenerOnFilePacketRequest(FileTransferPacketRequest request) {
        if (listener != null) {
            LOG.debug("Requesting packet: {}", request);
            listener.onFilePacketRequest(request);
        }
    }

    public interface Listener {
        void onStatus(FirmwareUpdateStatus status);

        void onFilePacketRequest(FileTransferPacketRequest request);
    }
}
