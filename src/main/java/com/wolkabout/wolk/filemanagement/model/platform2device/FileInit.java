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
package com.wolkabout.wolk.filemanagement.model.platform2device;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the payload sent by the platform to the device
 * to the `file_upload_initiate` endpoint to receive a new uploaded file.
 */
public class FileInit {

    @JsonProperty("name")
    private String fileName;

    @JsonProperty("size")
    private long fileSize;

    @JsonProperty("hash")
    private String fileHash;

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileName(String name) {
        this.fileName = name;
    }

    public void setFileSize(long size) {
        this.fileSize = size;
    }

    public void setFileHash(String hash) {
        this.fileHash = hash;
    }

    @Override
    public String toString() {
        return "FileInit{" +
                "name='" + fileName + '\'' +
                ", size=" + fileSize +
                ", hash='" + fileHash + '\'' +
                '}';
    }
}
