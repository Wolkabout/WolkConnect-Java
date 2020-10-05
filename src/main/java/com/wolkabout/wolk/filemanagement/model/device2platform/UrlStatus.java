/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk.filemanagement.model.device2platform;

import java.util.Objects;

/**
 * This class represents the payload sent by the device to the platform
 * to the `d2p/file_url_download_status/d/` endpoint to notify of the file download status.
 */
public class UrlStatus {

    private FileTransferStatus status;
    private FileTransferError error;
    private String fileName;
    private String fileUrl;

    public FileTransferStatus getStatus() {
        return status;
    }

    public void setStatus(FileTransferStatus status) {
        this.status = status;
    }

    public FileTransferError getError() {
        return error;
    }

    public void setError(FileTransferError error) {
        this.error = error;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "status=" + status +
                ", error=" + error +
                ", fileName='" + fileName +
                "', fileUrl='" + fileUrl +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlStatus)) return false;
        UrlStatus that = (UrlStatus) o;
        return error == that.error &&
                status == that.status && fileName.equals(that.fileName) && fileUrl.equals(that.fileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, error);
    }
}
