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
package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.apache.commons.lang3.StringUtils;
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

    public void setRepository(String repository) {
        LOG.info("Firmware update repository changed: " + repository);
        this.repository = repository;
    }

    public void setTimeAndReschedule(LocalTime time) {
        this.time = time;

        reschedule();
    }

    void schedule() {
        LOG.debug("Scheduling");

        if (time == null) {
            LOG.info("Firmware update not scheduled, time is not set");
            return;
        }

        long delay = computeExecutionDelay(LocalDateTime.now(), time);
        task = scheduler.schedule(this::updateAndSchedule, delay, TimeUnit.SECONDS);

        LOG.info("Firmware update scheduled at: " + LocalDateTime.now().plusSeconds(delay) + " from repository: " + repository);
    }

    void reschedule() {
        if (task == null) {
            LOG.debug("Task not set");
            schedule();
        } else if (task.getDelay(TimeUnit.NANOSECONDS) > 0) {
            LOG.debug("Task not started, canceling");
            task.cancel(false);
            schedule();
        } else {
            LOG.debug("Task started, is will schedule automatically");
        }
    }

    void updateAndSchedule() {
        LOG.debug("Running update task");

        try {
            checkAndInstall();
        } catch (Exception e) {
            LOG.error("Failed firmware update check: " + e.getMessage());
        } finally {
            schedule();
        }
    }

    private void checkAndInstall() {
        if (StringUtils.isEmpty(repository)) {
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

        return installer.isNewVersionAvailable(repository);
    }

    private void download() {
        LOG.debug("Downloading");

        fileProtocol.urlDownload(new UrlInfo(this.repository), this::handleDownloadFinish);
    }

    private void handleDownloadFinish(FileTransferStatus status, String fileName, FileTransferError error) {
        LOG.debug("Download finished");

        if (status != FileTransferStatus.FILE_READY) {
            LOG.warn("Stopping firmware update, file not ready");
            return;
        }

        install(fileName);
    }

    private void install(String fileName) {
        LOG.debug("Installing");

        firmwareProtocol.install(fileName);
    }

    long computeExecutionDelay(LocalDateTime now, LocalTime executeTime) {
        LocalDateTime nextRun = now.withHour(executeTime.getHour()).withMinute(executeTime.getMinute()).withSecond(executeTime.getSecond());
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        Duration duration = Duration.between(now, nextRun);

        return duration.getSeconds();
    }
}
