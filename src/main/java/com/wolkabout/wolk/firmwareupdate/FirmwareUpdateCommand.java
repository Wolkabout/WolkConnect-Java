/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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

    private FirmwareUpdateCommand(FirmwareUpdateCommand.Type type, String url) {
        this.type = type.toString();
        this.fileName = "";
        this.fileSize = 0L;
        this.base64FileSha256 = "";
        this.autoInstall = false;
        this.fileUrl = url;
    }

    public static FirmwareUpdateCommand fileUpload() {
        return new FirmwareUpdateCommand(Type.FILE_UPLOAD, "");
    }

    public static FirmwareUpdateCommand urlDownload(String url) {
        return new FirmwareUpdateCommand(Type.URL_DOWNLOAD, url);
    }

    public static FirmwareUpdateCommand install() {
        return new FirmwareUpdateCommand(Type.INSTALL, "");
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
