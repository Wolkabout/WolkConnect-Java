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
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FirmwareManagementTest {

    @Mock
    FirmwareInstaller firmwareInstallerMock;
    @Mock
    FirmwareUpdateProtocol firmwareUpdateProtocolMock;
    @Mock
    FileManagementProtocol fileManagementProtocolMock;

    FirmwareManagement firmwareManagement;

    @Before
    public void init() throws NoSuchFieldException {
        firmwareManagement = new FirmwareManagement(firmwareInstallerMock, firmwareUpdateProtocolMock, fileManagementProtocolMock);
    }

    @Test
    public void checkAndInstallEmptyRepo() throws NoSuchFieldException {
        firmwareManagement.checkAndInstall(null);

        verify(firmwareInstallerMock, times(0)).isNewVersionAvailable(anyString());
        verify(fileManagementProtocolMock, times(0)).urlDownload(any(UrlInfo.class), any(UrlFileDownloadSession.Callback.class));
    }

    @Test
    public void checkAndInstallNoNewVersion() throws NoSuchFieldException {
        doReturn(false).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        verify(firmwareInstallerMock, times(1)).isNewVersionAvailable("repo");
        verify(fileManagementProtocolMock, times(0)).urlDownload(any(UrlInfo.class), any(UrlFileDownloadSession.Callback.class));
    }

    @Test
    public void checkAndInstallNewVersion() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        verify(firmwareInstallerMock, times(1)).isNewVersionAvailable("repo");

        ArgumentCaptor<UrlInfo> argument = ArgumentCaptor.forClass(UrlInfo.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(argument.capture(), any(UrlFileDownloadSession.Callback.class));
        assertEquals(argument.getValue().getFileUrl(), "repo");
    }

    @Test
    public void downloadFinishedAborted() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.ABORTED, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedTransfer() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.FILE_TRANSFER, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedError() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.ERROR, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedUnknown() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.UNKNOWN, null, null);

        verify(firmwareUpdateProtocolMock, times(0)).install(anyString());
    }

    @Test
    public void downloadFinishedReady() throws NoSuchFieldException {
        doReturn(true).when(firmwareInstallerMock).isNewVersionAvailable(anyString());

        firmwareManagement.checkAndInstall("repo");

        ArgumentCaptor<UrlFileDownloadSession.Callback> argument = ArgumentCaptor.forClass(UrlFileDownloadSession.Callback.class);
        verify(fileManagementProtocolMock, times(1)).urlDownload(any(UrlInfo.class), argument.capture());

        argument.getValue().onFinish(FileTransferStatus.FILE_READY, "firmware", null);

        verify(firmwareUpdateProtocolMock, times(1)).install("firmware");
    }
}
