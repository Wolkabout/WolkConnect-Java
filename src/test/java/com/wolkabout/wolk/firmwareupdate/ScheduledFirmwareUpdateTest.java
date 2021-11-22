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
import com.wolkabout.wolk.filemanagement.UrlFileDownloadSession;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledFirmwareUpdateTest {

    @Mock
    FirmwareInstaller firmwareInstallerMock;
    @Mock
    FirmwareUpdateProtocol firmwareUpdateProtocolMock;
    @Mock
    FileManagementProtocol fileManagementProtocolMock;
    @Mock
    ScheduledExecutorService scheduledExecutorServiceMock;

    ScheduledFirmwareUpdate scheduledFirmwareUpdate;
    ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    @Before
    public void init() throws NoSuchFieldException {
        scheduledFirmwareUpdate = new ScheduledFirmwareUpdate(firmwareInstallerMock, firmwareUpdateProtocolMock, fileManagementProtocolMock, scheduledExecutorServiceMock);
    }

    void setupTime(LocalTime time) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("time"), time);
    }

    void setupRepository(String repository) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("repository"), repository);
    }

    void setupTask(ScheduledFuture task) throws NoSuchFieldException {
        FieldSetter.setField(scheduledFirmwareUpdate, scheduledFirmwareUpdate.getClass().getDeclaredField("task"), task);
    }

    @Test
    public void schedule() throws NoSuchFieldException {
        setupTime(LocalTime.of(10, 50));

        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        doReturn(100l).when(mock).computeExecutionDelay(any(LocalDateTime.class), any(LocalTime.class));

        mock.schedule();

        verify(scheduledExecutorServiceMock, times(1)).schedule(any(Runnable.class), eq(100l), eq(TimeUnit.SECONDS));
    }

    @Test
    public void scheduleNullTime() throws NoSuchFieldException {
        setupTime(null);

        scheduledFirmwareUpdate.schedule();

        verify(scheduledExecutorServiceMock, times(0)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void checkAndInstallEmptyRepo() throws NoSuchFieldException {
        setupRepository(null);

        scheduledFirmwareUpdate.checkAndInstall();

        verify(firmwareInstallerMock, times(0)).isNewVersionAvailable(anyString());
        verify(fileManagementProtocolMock, times(0)).urlDownload(any(UrlInfo.class), any(UrlFileDownloadSession.Callback.class));
    }

    @Test
    public void checkAndInstallNoNewVersion() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(false).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        verify(firmwareInstallerMock, times(1)).isNewVersionAvailable("repo");
        verify(fileManagementProtocolMock, times(0)).urlDownload(any(UrlInfo.class), any(UrlFileDownloadSession.Callback.class));
    }

    @Test
    public void checkAndInstallNewVersion() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        verify(firmwareInstallerMock, times(1)).isNewVersionAvailable("repo");

        ArgumentCaptor<UrlInfo> argument = ArgumentCaptor.forClass(UrlInfo.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(argument.capture(), any(UrlFileDownloadSession.Callback.class));
        assertEquals(argument.getValue().getFileUrl(), "repo");
    }

    @Test
    public void setTime() {
        ScheduledFirmwareUpdate mock = spy(scheduledFirmwareUpdate);

        mock.setTimeAndReschedule(LocalTime.of(5, 30));

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
    public void downloadFinishedAborted() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.ABORTED, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedTransfer() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.FILE_TRANSFER, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedError() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.ERROR, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedUnknown() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.UNKNOWN, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedReady() throws NoSuchFieldException {
        setupRepository("repo");

        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        scheduledFirmwareUpdate.checkAndInstall();

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.FILE_READY, "firmware", null);

        verify(firmwareUpdateProtocolMock, times(1)).install("firmware");
    }

    @Test
    public void computeDelayToday() throws NoSuchFieldException {
        LocalDateTime now = LocalDateTime.now();
        LocalTime schedule = LocalTime.of(now.getHour() + 1, now.getMinute(), now.getSecond());

        long delay = scheduledFirmwareUpdate.computeExecutionDelay(now, schedule);

        assertEquals(delay, TimeUnit.HOURS.toSeconds(1));
    }

    @Test
    public void computeDelayTomorow() throws NoSuchFieldException {
        LocalDateTime now = LocalDateTime.now();
        LocalTime schedule = LocalTime.of(now.getHour() - 1, now.getMinute(), now.getSecond());

        long delay = scheduledFirmwareUpdate.computeExecutionDelay(now, schedule);

        assertEquals(delay, TimeUnit.HOURS.toSeconds(23));
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
