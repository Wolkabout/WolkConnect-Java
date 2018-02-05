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

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(value = "command")
    private String type;

    @JsonProperty(value = "fileName")
    private String fileName;

    @JsonProperty(value = "fileSize")
    private Long fileSize;

    @JsonProperty(value = "fileHash")
    private String base64FileSha256;

    @JsonProperty(value = "autoInstall")
    private boolean autoInstall;

    @JsonProperty(value = "fileUrl")
    private String fileUrl;

    // Required by Jackson
    private FirmwareUpdateCommand() {
    }

    private FirmwareUpdateCommand(FirmwareUpdateCommand.Type type) {
        this.type = type.toString();
    }

    private FirmwareUpdateCommand(FirmwareUpdateCommand.Type type, String fileName, long fileSize, String base64FileSha256, boolean autoInstall) {
        this.type = type.toString();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.base64FileSha256 = base64FileSha256;
        this.autoInstall = autoInstall;
    }

    private FirmwareUpdateCommand(FirmwareUpdateCommand.Type type, String fileUrl, boolean autoInstall) {
        this.type = type.toString();
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
        try {
            return FirmwareUpdateCommand.Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown command: {}", type);
            return FirmwareUpdateCommand.Type.UNKNOWN;
        }
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
