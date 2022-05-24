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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.cronutils.model.field.expression.FieldExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledFirmwareUpdateTest {

    @Mock
    FirmwareManagement firmwareManagementMock;
    @Mock
    ScheduledExecutorService scheduledExecutorServiceMock;

    ScheduledFirmwareUpdate scheduledFirmwareUpdate;
    ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    @Before
    public void init() throws NoSuchFieldException {
        scheduledFirmwareUpdate = new ScheduledFirmwareUpdate(firmwareManagementMock, scheduledExecutorServiceMock);
    }

    void setupCron(Cron cron) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("cron"), cron);
    }

    void setupRepository(String repository) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("repository"), repository);
    }

    void setupTask(ScheduledFuture task) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("task"), task);
    }

    @Test
    public void schedule() throws NoSuchFieldException {

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoM(always())
                .withMonth(always())
                .withDoW(questionMark())
                .withHour(on(10))
                .withMinute(on(50))
                .withSecond(on(0))
                .instance();

        setupCron(cron);

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doReturn(100l).when(mock).computeExecutionDelay(any(LocalDateTime.class), any(Cron.class));

        mock.schedule();

        verify(scheduledExecutorServiceMock, times(1)).schedule(any(Runnable.class), eq(100l), eq(TimeUnit.SECONDS));
    }

    @Test
    public void scheduleNullTime() throws NoSuchFieldException {
        setupCron(null);

        scheduledFirmwareUpdate.schedule();

        verify(scheduledExecutorServiceMock, times(0)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void setTime() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoM(always())
                .withMonth(always())
                .withDoW(questionMark())
                .withHour(on(5))
                .withMinute(on(30))
                .withSecond(on(0))
                .instance();

        mock.setTimeAndReschedule(cron);

        verify(mock, times(1)).reschedule();
    }

    @Test
    public void rescheduleTaskNull() throws NoSuchFieldException {
        setupTask(null);

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);
        doNothing().when(mock).schedule();

        mock.reschedule();

        verify(mock, times(1)).schedule();
    }

    @Test
    public void rescheduleTaskNotStarted() throws NoSuchFieldException {
        ScheduledFuture task = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                // do nothing
            }
        }, 5, TimeUnit.SECONDS);

        setupTask(task);

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);
        doNothing().when(mock).schedule();

        mock.reschedule();

        verify(mock, times(1)).schedule();
    }

    @Test
    public void rescheduleTaskStarted() throws NoSuchFieldException, InterruptedException {
        ScheduledFuture task = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 1, TimeUnit.SECONDS);

        // be sure the task is started
        TimeUnit.SECONDS.sleep(2);

        setupTask(task);

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        mock.reschedule();

        verify(mock, times(0)).schedule();
    }

    @Test
    public void setTimeNull() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        mock.setTimeAndReschedule(null);

        verify(mock, times(1)).reschedule();
    }

    @Test
    public void setRepo() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        mock.setRepository("");

        verify(mock, times(0)).reschedule();
    }

    @Test
    public void computeDelayToday() throws NoSuchFieldException {
        LocalDateTime now = LocalDateTime.now();

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoM(always())
                .withMonth(always())
                .withDoW(questionMark())
                .withHour(on(now.getHour() + 1))
                .withMinute(on(now.getMinute()))
                .withSecond(on(now.getSecond()))
                .instance();

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doReturn(0l).when(mock).randomDelay();

        long delay = mock.computeExecutionDelay(now, cron);

        assertEquals(delay, TimeUnit.HOURS.toSeconds(1) - 1);
    }

    @Test
    public void computeDelayTomorow() throws NoSuchFieldException {
        LocalDateTime now = LocalDateTime.now();

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoM(always())
                .withMonth(always())
                .withDoW(questionMark())
                .withHour(on(now.getHour() - 1))
                .withMinute(on(now.getMinute()))
                .withSecond(on(now.getSecond()))
                .instance();

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doReturn(0l).when(mock).randomDelay();

        long delay = mock.computeExecutionDelay(now, cron);

        assertEquals(delay, TimeUnit.HOURS.toSeconds(23) - 1);
    }

    @Test
    public void updateAndScheduleSuccess() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doNothing().when(mock).checkAndInstall();

        mock.updateAndSchedule();

        verify(mock, times(1)).schedule();
    }

    @Test
    public void updateAndScheduleFail() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doThrow(new IllegalArgumentException()).when(mock).checkAndInstall();

        mock.updateAndSchedule();

        verify(mock, times(1)).schedule();
    }
}
