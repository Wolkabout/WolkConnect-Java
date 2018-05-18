/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;

public enum UpdateError {

    /**
     * An error not understood by the current protocol version.
     */
    UNKNOWN_ERROR(-1),

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

    public static UpdateError byCode(int code) {
        for (UpdateError error : values()) {
            if (error.code == code) {
                return error;
            }
        }

        return UNKNOWN_ERROR;
    }
}
