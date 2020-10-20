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
 * All possible values found in the `error` value for status firmware update messages.
 */
public enum FirmwareUpdateError {

    // An unexpected error occurred.
    UNSPECIFIED_ERROR(0),

    // Requested file is not found on the device.
    FILE_NOT_PRESENT(1),

    // Error that occurred is caused by the file system.
    FILE_SYSTEM_ERROR(2),

    // The installation process has was not successful.
    INSTALLATION_FAILED(3),

    // The child device targeted is not present.
    // Exists by protocol, but does not have a use in a connector.
    SUBDEVICE_NOT_PRESENT(4);

    // Error code value as int
    private final int code;

    FirmwareUpdateError(int code) {
        this.code = code;
    }
}
