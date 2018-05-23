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

package com.wolkabout.wolk.firmwareupdate.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileInfo {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final String command = "FILE_UPLOAD";

    private String fileName;
    private long fileSize;
    private String fileHash;
    private boolean autoInstall;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public boolean isAutoInstall() {
        return autoInstall;
    }

    public void setAutoInstall(boolean autoInstall) {
        this.autoInstall = autoInstall;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "command='" + command + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileHash='" + fileHash + '\'' +
                ", autoInstall=" + autoInstall +
                '}';
    }
}
