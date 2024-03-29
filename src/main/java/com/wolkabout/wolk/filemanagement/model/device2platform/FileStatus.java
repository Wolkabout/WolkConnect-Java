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
package com.wolkabout.wolk.filemanagement.model.device2platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;

/**
 * This class represents the payload sent by the device to the platform
 * to the `d2p/file_upload_status/d/` endpoint to notify of the file upload status.
 */
public class FileStatus {

    @JsonProperty("name")
    private String fileName;

    @JsonProperty("status")
    private FileTransferStatus status;

    @JsonProperty("error")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FileTransferError error;

    public FileStatus(String fileName, FileTransferStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("FileTransferStatus can not be null.");
        }

        this.fileName = fileName;
        this.status = status;
    }

    public FileStatus(String fileName, FileTransferStatus status, FileTransferError error) {
        if (status == null) {
            throw new IllegalArgumentException("FileTransferStatus can not be null.");
        }

        this.fileName = fileName;
        this.status = status;
        this.error = error;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

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

    @Override
    public String toString() {
        return "FileStatus={" +
                "name='" + fileName + '\'' +
                ", status='" + status.name() + '\'' +
                (error != null ? (", error='" + error.name() + '\'') : "") + '}';
    }
}
