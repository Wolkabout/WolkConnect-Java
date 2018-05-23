/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model.command;

import com.wolkabout.wolk.firmwareupdate.model.command.Command;

public class UrlInfo extends Command {

    private String fileUrl;
    private boolean autoInstall;

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
                "fileUrl='" + fileUrl + '\'' +
                ", autoInstall=" + autoInstall +
                "} " + super.toString();
    }
}
