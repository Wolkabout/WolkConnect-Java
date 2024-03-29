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
 * Possible statuses for file management.
 */
public enum FileTransferStatus {

    // The platform initiated the upload but the device hasn’t responded yet.
    // Not included since it should not be sent by device
    // AWAITING_DEVICE,

    // Transfer still in progress
    FILE_TRANSFER,

    // File ready to be installed
    FILE_READY,

    // Error occurred during transfer
    ERROR,

    // Aborted from the server
    ABORTED,

    // Unknown error
    UNKNOWN
}
