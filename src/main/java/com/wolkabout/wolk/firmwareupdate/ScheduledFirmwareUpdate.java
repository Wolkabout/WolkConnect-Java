package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledFirmwareUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledFirmwareUpdate.class);

    private final FirmwareInstaller installer;
    private final FirmwareUpdateProtocol firmwareProtocol;
    private final FileManagementProtocol fileProtocol;

    private LocalTime time;
    private String repository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture task;

    public ScheduledFirmwareUpdate(FirmwareInstaller installer, FirmwareUpdateProtocol firmwareProtocol, FileManagementProtocol fileProtocol) {
        this(installer, firmwareProtocol, fileProtocol, null, null);
    }

    public ScheduledFirmwareUpdate(FirmwareInstaller installer, FirmwareUpdateProtocol firmwareProtocol, FileManagementProtocol fileProtocol, String repository, LocalTime time) {
        if (installer == null) {
            throw new IllegalArgumentException("The firmware installer cannot be null.");
        }

        if (firmwareProtocol == null) {
            throw new IllegalArgumentException("The firmware update protocol cannot be null.");
        }

        if (fileProtocol == null) {
            throw new IllegalArgumentException("The file management protocol cannot be null.");
        }

        this.installer = installer;
        this.firmwareProtocol = firmwareProtocol;
        this.fileProtocol = fileProtocol;
        this.repository = repository;
        this.time = time;

        schedule();
    }


    public void setTime(LocalTime time) {
        this.time = time;

        schedule();
    }

    public void setRepository(String repository) {
        LOG.info("Firmware update repository changed: " + repository);
        this.repository = repository;
    }

    private void schedule() {
        if (task != null) {
            task.cancel(false);
        }

        if (time == null) {
            LOG.info("Firmware update not scheduled");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(time.getHour()).withMinute(time.getMinute()).withSecond(time.getSecond());
        if (now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long delay = duration.getSeconds();

        task = scheduler.schedule(this::checkAndInstall, delay, TimeUnit.SECONDS);

        LOG.info("Firmware update scheduled at: " + nextRun + " from repository: " + repository);
    }

    private void checkAndInstall() {
        if (repository == null || repository.isEmpty()) {
            LOG.warn("Skipping update, repository not defined");
            return;
        }

        if (!shouldUpdate()) {
            LOG.info("New firmware version not available");
            return;
        }

        LOG.info("New firmware version available");

        download();
    }

    private boolean shouldUpdate() {
        LOG.info("Checking for new firmware version");

        return installer.versionsDifferent(repository);
    }

    private void download() {
        fileProtocol.urlDownload(new UrlInfo(this.repository), this::handleDownloadFinish);
    }

    private void handleDownloadFinish(FileTransferStatus status, String fileName, FileTransferError error) {
        if (status != FileTransferStatus.FILE_READY) {
            LOG.warn("Stopping firmware update, file not ready");
            return;
        }

        install(fileName);
    }

    private void install(String fileName) {
        firmwareProtocol.install(fileName);
    }
}
