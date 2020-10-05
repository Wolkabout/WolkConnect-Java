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

/**
 * All possible values found in the `error` value for status transfer messages.
 */
public enum FileTransferError {

    // An unexpected error occurred.
    UNSPECIFIED_ERROR(0),

    // Requested file transfer protocol is not supported on the device.
    TRANSFER_PROTOCOL_DISABLED(1),

    // File size is greater than that supported by the device.
    UNSUPPORTED_FILE_SIZE(2),

    // Given file URL is malformed.
    MALFORMED_URL(3),

    // File with the same name but different hash is already present on the device.
    FILE_HASH_MISMATCH(4),

    // Firmware file cannot be handled on the device due to a file system error.
    FILE_SYSTEM_ERROR(5),

    // A device has failed to recover from error 3 times in a row during upload/download.
    RETRY_COUNT_EXCEEDED(10);

    // Convert the enum value into string
    public static String toString(FileTransferError value) {
        return value.name();
    }

    // Convert from string into an enum value
    public static FileTransferError fromString(String value) {
        for (FileTransferError error : values()) {
            if (error.name().equalsIgnoreCase(value)) {
                return error;
            }
        }

        return UNSPECIFIED_ERROR;
    }

    // Error code value as int
    private final int code;

    FileTransferError(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
