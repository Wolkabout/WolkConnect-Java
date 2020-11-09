package com.wolkabout.wolk.filemanagement;

import com.sun.tools.javac.util.Pair;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;

public interface UrlFileDownloader {

    Pair<FileTransferStatus, FileTransferError> downloadFile(String fileUrl);
}
