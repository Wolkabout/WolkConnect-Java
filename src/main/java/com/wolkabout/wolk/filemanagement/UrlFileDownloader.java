package com.wolkabout.wolk.filemanagement;

import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;

import java.util.Map;

public interface UrlFileDownloader {

    Map.Entry<FileTransferStatus, FileTransferError> downloadFile(String fileUrl);
}
