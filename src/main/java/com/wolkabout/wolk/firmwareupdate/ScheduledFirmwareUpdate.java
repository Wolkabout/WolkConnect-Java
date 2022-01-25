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

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledFirmwareUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledFirmwareUpdate.class);

    private final FirmwareManagement firmwareManagement;

    private Cron cron;
    private String repository;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture task;

    private static final long RANDOMIZED_MAX_DELAY = TimeUnit.MINUTES.toSeconds(15);

    public ScheduledFirmwareUpdate(FirmwareManagement firmwareManagement, ScheduledExecutorService scheduler) {
        this(firmwareManagement, scheduler, null, null);
    }

    public ScheduledFirmwareUpdate(FirmwareManagement firmwareManagement, ScheduledExecutorService scheduler, String repository, Cron cron) {
        this.firmwareManagement = firmwareManagement;
        this.scheduler = scheduler;
        this.repository = repository;
        this.cron = cron;

        schedule();
    }

    public void setRepository(String repository) {
        LOG.info("Firmware update repository changed: " + repository);
        this.repository = repository;
    }

    public void setTimeAndReschedule(Cron cron) {
        this.cron = cron;

        reschedule();
    }

    void schedule() {
        LOG.debug("Scheduling");

        if (cron == null) {
            LOG.info("Firmware update not scheduled, time is not set");
            return;
        }

        long secondsUntilExecution = computeExecutionDelay(LocalDateTime.now(), cron);

        task = scheduler.schedule(this::updateAndSchedule, secondsUntilExecution, TimeUnit.SECONDS);

        LOG.info("Firmware update scheduled at: " + LocalDateTime.now().plusSeconds(secondsUntilExecution) + " from repository: " + repository);
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
            LOG.debug("Task started, it will schedule automatically");
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

    void checkAndInstall() {
        if (StringUtils.isEmpty(repository)) {
            LOG.warn("Skipping update, repository not defined");
            return;
        }

        firmwareManagement.checkAndInstall(repository);
    }

    long computeExecutionDelay(LocalDateTime now, Cron cron) {

        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        Optional<Duration> nextExecution = executionTime.timeToNextExecution(ZonedDateTime.from(now.atZone(ZoneId.systemDefault())));

        if (nextExecution.isPresent()) {
            return nextExecution.get().getSeconds() + randomDelay();
        }

        LOG.warn("Unable to calculate next execution, using default value of 1 day");

        Optional<Duration> lastExecution = executionTime.timeFromLastExecution(ZonedDateTime.from(now));

        return lastExecution.map(duration -> TimeUnit.DAYS.toSeconds(1) - duration.getSeconds()).orElseGet(() -> TimeUnit.DAYS.toSeconds(1)) + randomDelay();
    }

    long randomDelay() {
        return (long) (Math.random() * RANDOMIZED_MAX_DELAY);
    }
}
