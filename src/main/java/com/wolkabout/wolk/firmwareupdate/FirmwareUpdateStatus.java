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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FirmwareUpdateStatus {
    public enum StatusCode {

        @JsonProperty("FILE_TRANSFER")
        FILE_TRANSFER,

        @JsonProperty("FILE_READY")
        FILE_READY,

        @JsonProperty("INSTALLATION")
        INSTALLATION,

        @JsonProperty("COMPLETED")
        INSTALLATION_COMPLETED,

        @JsonProperty("ABORTED")
        ABORTED,

        @JsonProperty("ERROR")
        ERROR
    }

    public enum ErrorCode {
        UNSPECIFIED(0),
        FILE_UPLOAD_DISABLED(1),
        UNSUPPORTED_FILE_SIZE(2),
        INSTALLATION_FAILED(3),
        MALFORMED_URL(4),
        FILE_SYSTEM_ERROR(5),
        RETRY_COUNT_EXCEEDED(10);

        private int value;

        ErrorCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private StatusCode status;

    private Integer errorCode;

    private FirmwareUpdateStatus(StatusCode status) {
        this.status = status;
        this.errorCode = null;
    }

    private FirmwareUpdateStatus(StatusCode status, ErrorCode errorCode) {
        this.status = status;
        this.errorCode = errorCode.getValue();
    }

    public static FirmwareUpdateStatus ok(StatusCode status) {
        return new FirmwareUpdateStatus(status);
    }

    public static FirmwareUpdateStatus error(ErrorCode errorCode) {
        return new FirmwareUpdateStatus(StatusCode.ERROR, errorCode);
    }

    @JsonProperty(value = "status")
    public StatusCode getStatus() {
        return status;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "error")
    public Integer getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "FirmwareUpdateStatus{" +
                "status=" + status +
                ", errorCode=" + errorCode +
                '}';
    }
}
