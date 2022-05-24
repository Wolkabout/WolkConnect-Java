package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirmwareManagement {

    private static final Logger LOG = LoggerFactory.getLogger(FirmwareManagement.class);

    private final FirmwareInstaller installer;
    private final FirmwareUpdateProtocol firmwareProtocol;
    private final FileManagementProtocol fileProtocol;

    public FirmwareManagement(FirmwareInstaller installer, FirmwareUpdateProtocol firmwareProtocol, FileManagementProtocol fileProtocol) {
        this.installer = installer;
        this.firmwareProtocol = firmwareProtocol;
        this.fileProtocol = fileProtocol;
    }

    public void checkAndInstall(String repository) {
        if (StringUtils.isEmpty(repository)) {
            LOG.warn("Skipping update, repository not defined");
            return;
        }

        if (!shouldUpdate(repository)) {
            LOG.info("New firmware version not available");
            return;
        }

        LOG.info("New firmware version available");

        download(repository);
    }

    private boolean shouldUpdate(String repository) {
        LOG.info("Checking for new firmware version");

        return installer.isNewVersionAvailable(repository);
    }

    private void download(String repository) {
        LOG.debug("Downloading");

        fileProtocol.urlDownload(new UrlInfo(repository), this::handleDownloadFinish);
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
}
