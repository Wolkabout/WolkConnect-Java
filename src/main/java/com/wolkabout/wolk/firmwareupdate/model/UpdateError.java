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

public enum UpdateError {

    /**
     * An error not predicted by the protocol.
     */
    UNSPECIFIED_ERROR(0),

    /**
     * File upload disabled on the device.
     */
    FILE_UPLOAD_DISABLED(1),

    /**
     * File size was not supported by the device
     */
    UNSUPPORTED_FILE_SIZE(2),

    /**
     * Error occurred while installing firmware
     */
    INSTALLATION_FAILED(3),

    /**
     * Given file URL is malformed.
     */
    MALFORMED_URL(4),

    /**
     * File system error.
     */
    FILE_SYSTEM_ERROR(5),

    /**
     * The protocol tried to auto recover from an error 3 times.
     */
    RETRY_COUNT_EXCEEDED(10);

    private int code;

    UpdateError(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
