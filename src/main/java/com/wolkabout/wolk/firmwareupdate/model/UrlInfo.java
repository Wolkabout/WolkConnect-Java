/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UrlInfo {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final String command = "URL_DOWNLOAD";

    private String fileUrl;
    private boolean autoInstall;

    public String getCommand() {
        return command;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public boolean isAutoInstall() {
        return autoInstall;
    }

    public void setAutoInstall(boolean autoInstall) {
        this.autoInstall = autoInstall;
    }

    @Override
    public String toString() {
        return "UrlInfo{" +
                "command='" + command + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", autoInstall=" + autoInstall +
                '}';
    }
}
