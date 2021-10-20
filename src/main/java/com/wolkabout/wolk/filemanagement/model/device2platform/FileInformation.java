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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the payload sent by the device to the platform
 * used in arrays to be send to 'd2p/file_list_response/d/' or 'd2p/file_list_update/d/'.
 */
public class FileInformation {
    @JsonProperty("name")
    private String fileName;

    @JsonProperty("size")
    private long fileSize;

    @JsonProperty("hash")
    private String fileHash;

    public FileInformation(String fileName, long fileSize, String fileHash) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return fileSize;
    }

    public void setSize(long size) {
        this.fileSize = size;
    }

    public String getHash() {
        return fileHash;
    }

    public void setHash(String hash) {
        this.fileHash = hash;
    }

    @Override
    public String toString() {
        return "FileInformation={" +
                "name='" + fileName + '\'' +
                ", size=" + fileSize +
                ", hash='" + fileHash + '\'' +
                '}';
    }
}
