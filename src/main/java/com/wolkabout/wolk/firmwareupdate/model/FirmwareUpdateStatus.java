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
public enum FirmwareUpdateStatus {

    // Command has been issued, but the device hasn’t accepted the command yet.
    // Not included since it should not be sent by device
    // AWAITING_DEVICE,

    // Installation is still in progress
    INSTALLING,

    //  The installation was completed successfully
    SUCCESS,

    // Error occurred during installation
    ERROR,

    // Aborted from the server
    ABORTED,

    // Unknown error
    UNKNOWN
}
