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

public class ScheduledUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledUpdate.class);

    private final FirmwareInstaller installer;
    private final FirmwareUpdateProtocol firmwareProtocol;
    private final FileManagementProtocol fileProtocol;

    private LocalTime time;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture task;

    public ScheduledUpdate(FirmwareInstaller installer, FirmwareUpdateProtocol firmwareProtocol, FileManagementProtocol fileProtocol, LocalTime time) {
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
        this.time = time;

        schedule();
    }


    public void setTime(LocalTime time) {
        this.time = time;

        schedule();
    }

    public void schedule() {
        if (task != null) {
            task.cancel(false);
        }

        if (time == null) {
            LOG.info("Firmware update schedule stopped");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(time.getHour()).withMinute(time.getMinute()).withSecond(time.getSecond());
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long delay = duration.getSeconds();

        task = scheduler.schedule(this::checkAndInstall, delay, TimeUnit.SECONDS);
    }

    private void checkAndInstall() {

        String newFirmwareUrl = checkVersion();

        if (newFirmwareUrl == null || newFirmwareUrl.isEmpty()) {
            return;
        }

        LOG.info("New firmware version available.");

        download(newFirmwareUrl);
    }

    private String checkVersion() {
        LOG.info("Checking for new firmware version.");

        return installer.checkNewVersion();
    }

    private void download(String url) {
        fileProtocol.urlDownload(new UrlInfo(url), this::handleDownloadFinish);
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
