package com.wolkabout.wolk.filemanagement;

import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;

public class UrlFileDownloadResult {
    private final FileTransferStatus status;
    private final FileTransferError error;
    private final String fileName;

    public UrlFileDownloadResult(FileTransferStatus status, String fileName) {
        this.status = status;
        this.error = null;
        this.fileName = fileName;
    }

    public UrlFileDownloadResult(FileTransferError error) {
        this.status = FileTransferStatus.ERROR;
        this.error = error;
        this.fileName = "";
    }

    public FileTransferStatus getStatus() {
        return status;
    }

    public FileTransferError getError() {
        return error;
    }

    public String getFileName() {
        return fileName;
    }
}
