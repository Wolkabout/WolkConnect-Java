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

package com.wolkabout.wolk.firmwareupdate.model;

/**
 * Possible statuses for firmware update.
 */
public enum FirmwareStatus {

    /**
     * File transfer in progress.
     */
    FILE_TRANSFER,

    /**
     * File transfer completed.
     */
    FILE_READY,

    /**
     * Firmware installation in progress.
     */
    INSTALLATION,

    /**
     * Firmware update completed successfully.
     */
    COMPLETED,

    /**
     * Error during firmware update.
     */
    ERROR,

    /**
     * Aborted by user.
     */
    ABORTED,

    /**
     * Unknown state encountered.
     */
    UNKNOWN;

    public static FirmwareStatus fromString(String value) {
        for (FirmwareStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        return UNKNOWN;
    }
}
