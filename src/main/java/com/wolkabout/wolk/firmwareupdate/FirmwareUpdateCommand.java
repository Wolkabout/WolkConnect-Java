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
package com.wolkabout.wolk.firmwareupdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirmwareUpdateCommand {
    private static final Logger LOG = LoggerFactory.getLogger(FirmwareUpdateCommand.class);

    public enum Type {
        FILE_UPLOAD,
        URL_DOWNLOAD,
        INSTALL,
        ABORT,

        UNKNOWN
    }

    private Type type;

    private String fileName;

    private Long fileSize;

    private String base64FileSha256;

    private boolean autoInstall;

    private String fileUrl;

    public FirmwareUpdateCommand(FirmwareUpdateCommand.Type type) {
        this.type = type;
    }

    public FirmwareUpdateCommand(FirmwareUpdateCommand.Type type, String fileName, long fileSize, String base64FileSha256, boolean autoInstall) {
        this.type = type;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.base64FileSha256 = base64FileSha256;
        this.autoInstall = autoInstall;
    }

    public FirmwareUpdateCommand(FirmwareUpdateCommand.Type type, String fileUrl, boolean autoInstall) {
        this.type = type;
        this.fileUrl = fileUrl;
        this.autoInstall = autoInstall;
    }

    public static FirmwareUpdateCommand fileUpload(String fileName, long fileSize, String base64FileSha256) {
        return new FirmwareUpdateCommand(Type.FILE_UPLOAD, fileName, fileSize, base64FileSha256, false);
    }

    public static FirmwareUpdateCommand urlDownload(String url, boolean autoInstall) {
        return new FirmwareUpdateCommand(Type.URL_DOWNLOAD, url, false);
    }

    public static FirmwareUpdateCommand install() {
        return new FirmwareUpdateCommand(Type.INSTALL);
    }

    public static FirmwareUpdateCommand abort() {
        return new FirmwareUpdateCommand(Type.ABORT);
    }

    public FirmwareUpdateCommand.Type getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getBase64FileSha256() {
        return base64FileSha256;
    }

    public boolean getAutoInstall() {
        return autoInstall;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}
