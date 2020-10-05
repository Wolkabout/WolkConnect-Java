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
 * Possible statuses for firmware update.
 */
public enum FileTransferStatus {

    // Transfer still in progress
    FILE_TRANSFER,

    // File ready to be installed
    FILE_READY,

    // Error occurred during transfer
    ERROR,

    // Aborted from the server
    ABORTED,

    // Unknown error
    UNKNOWN;

    // Convert the enum value into string
    public static String toString(FileTransferStatus value) {
        return value.name();
    }

    // Convert from string into an enum value
    public static FileTransferStatus fromString(String value) {
        for (FileTransferStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        return UNKNOWN;
    }
}
