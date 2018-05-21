/*
 * Copyright (c) 2018 Wolkabout
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
