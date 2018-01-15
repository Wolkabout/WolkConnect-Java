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
package com.wolkabout.wolk;

import com.wolkabout.wolk.firmwareupdate.FirmwareUpdate;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateCommand;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class FirmwareUpdateTest {

    private static final File SAVED_FIRMWARE_VERSION_FILE = Paths.get(".dfu-version").toFile();

    private FirmwareUpdate.Listener firmwareUpdateListener;

    private FirmwareUpdateHandler firmwareUpdateHandler;

    private FirmwareDownloadHandler firmwareDownloadHandler;

    @Before
    public void setUp() {
        firmwareUpdateListener = mock(FirmwareUpdate.Listener.class);

        firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);

        firmwareDownloadHandler = mock(FirmwareDownloadHandler.class);
    }


    @After
    public void tearDown() {
        if (SAVED_FIRMWARE_VERSION_FILE.exists()) {
            SAVED_FIRMWARE_VERSION_FILE.delete();
        }
    }

    @Test
    public void Given_FirmwareUpdateWithoutFirmwareUpdateHandler_When_FileTransferFirmwareUpdateIsRequested_Then_FileUploadDisabledErrorIsYielded() {
        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, null, null);
        firmwareUpdate.setListener(firmwareUpdateListener);

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.fileUpload());

        // Then
        verify(firmwareUpdateListener).onStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
    }

    @Test
    public void Given_FirmwareUpdateWithoutFirmwareUpdateHandler_When_UrlDownloadFirmwareUpdateIsRequested_Then_FileUploadDisabledErrorIsYielded() {
        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, null, null);
        firmwareUpdate.setListener(firmwareUpdateListener);

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload(""));

        // Then
        verify(firmwareUpdateListener).onStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
    }

    @Test
    public void Given_FirmwareUpdateWithMaximumFirmwareSizeOfZero_When_FileTransferFirmwareUpdateIsRequested_Then_FileUploadDisabledErrorIsYielded() {
        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, null);
        firmwareUpdate.setListener(firmwareUpdateListener);

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.fileUpload());

        // Then
        verify(firmwareUpdateListener).onStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
    }

    @Test
    public void Given_FirmwareUpdateWithoutUrlDownloaded_When_UrlDownloadFirmwareUpdateIsRequested_Then_FileUploadDisabledErrorIsYielded() {
        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, null);
        firmwareUpdate.setListener(firmwareUpdateListener);

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload(""));

        // Then
        verify(firmwareUpdateListener).onStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.FILE_UPLOAD_DISABLED));
    }

    @Test
    public void Given_FirmwareUpdateWithUrlDownload_When_MalformedUrlDownloadFirmwareUpdateIsRequested_Then_MalformedUrlErrorIsYielded() {
        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, firmwareDownloadHandler);
        firmwareUpdate.setListener(firmwareUpdateListener);

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload("Obviously not valid URL"));

        // Then
        verify(firmwareUpdateListener).onStatus(FirmwareUpdateStatus.error(FirmwareUpdateStatus.ErrorCode.MALFORMED_URL));
    }

    @Test
    public void Given_FirmwareUpdateWithUrlDownload_When_UrlDownloadFirmwareUpdateIsRequested_Then_FileTransferStatusIsYielded() throws InterruptedException, IOException {
        final URL fileUrl = new URL("file:///firmware_file_to_download");

        final FirmwareUpdateStatusAggregator firmwareUpdateStatusAggregator = new FirmwareUpdateStatusAggregator();

        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, firmwareDownloadHandler);
        firmwareUpdate.setListener(firmwareUpdateStatusAggregator);
        firmwareUpdate.setAbortTimePeriod(0);

        when(firmwareDownloadHandler.downloadFile(fileUrl)).thenReturn(Paths.get("./downloaded_file_path"));

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload("file:///firmware_file_to_download"));

        // Then
        final List<FirmwareUpdateStatus> firmwareUpdateStatuses = firmwareUpdateStatusAggregator.waitFor(1);
        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_TRANSFER), firmwareUpdateStatuses.get(0));
    }

    @Test
    public void Given_FirmwareUpdateWithUrlDownload_When_UrlDownloadFirmwareUpdateIsDone_Then_FileReadyStatusIsYielded() throws InterruptedException, IOException {
        final URL fileUrl = new URL("file:///firmware_file_to_download");

        final FirmwareUpdateStatusAggregator firmwareUpdateStatusAggregator = new FirmwareUpdateStatusAggregator();

        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, firmwareDownloadHandler);
        firmwareUpdate.setListener(firmwareUpdateStatusAggregator);
        firmwareUpdate.setAbortTimePeriod(0);

        when(firmwareDownloadHandler.downloadFile(fileUrl)).thenReturn(Paths.get("./downloaded_file_path"));

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload(fileUrl.toString()));

        // Then
        final List<FirmwareUpdateStatus> firmwareUpdateStatuses = firmwareUpdateStatusAggregator.waitFor(2);
        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_TRANSFER), firmwareUpdateStatuses.get(0));
        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_READY), firmwareUpdateStatuses.get(1));
    }

    @Test
    public void Given_DownloadedFirmware_When_InstallCommandIsIssued_Then_InstallationStatusIsYieldedAndFirmwareUpdateHandlerIsInvoked() throws InterruptedException, IOException {
        final URL fileUrl = new URL("file:///firmware_file_to_download");
        final Path downloadedFirmwareFile = Paths.get("./downloaded_firmware_file");

        final FirmwareUpdateStatusAggregator firmwareUpdateStatusAggregator = new FirmwareUpdateStatusAggregator();

        // Given
        final FirmwareUpdate firmwareUpdate = new FirmwareUpdate("", Paths.get(""), 0, firmwareUpdateHandler, firmwareDownloadHandler);
        firmwareUpdate.setListener(firmwareUpdateStatusAggregator);
        firmwareUpdate.setAbortTimePeriod(0);

        when(firmwareDownloadHandler.downloadFile(fileUrl)).thenReturn(downloadedFirmwareFile);

        firmwareUpdate.handleCommand(FirmwareUpdateCommand.urlDownload(fileUrl.toString()));
        List<FirmwareUpdateStatus> firmwareUpdateStatuses = firmwareUpdateStatusAggregator.waitFor(2);

        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_TRANSFER), firmwareUpdateStatuses.get(0));
        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.FILE_READY), firmwareUpdateStatuses.get(1));

        // When
        firmwareUpdate.handleCommand(FirmwareUpdateCommand.install());

        // Then
        firmwareUpdateStatuses = firmwareUpdateStatusAggregator.waitFor(1);
        assertEquals(FirmwareUpdateStatus.ok(FirmwareUpdateStatus.StatusCode.INSTALLATION), firmwareUpdateStatuses.get(0));

        verify(firmwareUpdateHandler).updateFirmwareWithFile(downloadedFirmwareFile);
    }
}
