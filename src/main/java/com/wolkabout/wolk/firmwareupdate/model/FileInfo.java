/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;

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
