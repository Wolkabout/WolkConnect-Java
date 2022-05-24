/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk.filemanagement.model;

/**
 * All possible values found in the `error` value for status transfer messages.
 */
public enum FileTransferError {

    // An unexpected error occurred.
    UNKNOWN,

    // Requested file transfer protocol is not supported on the device.
    TRANSFER_PROTOCOL_DISABLED,

    // File size is greater than that supported by the device.
    UNSUPPORTED_FILE_SIZE,

    // Given file URL is malformed.
    MALFORMED_URL,

    // File with the same name but different hash is already present on the device.
    FILE_HASH_MISMATCH,

    // Firmware file cannot be handled on the device due to a file system error.
    FILE_SYSTEM_ERROR,

    // A device has failed to recover from error 3 times in a row during upload/download.
    RETRY_COUNT_EXCEEDED;
}
