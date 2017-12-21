/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk;

import java.nio.file.Path;

/**
 * Provide implementation of {@link FirmwareUpdateHandler} to enable OTA updates for device.
 */
public interface FirmwareUpdateHandler {
    /**
     * When firmware file is received from WolkAbout IoT platform {@link #updateFirmwareWithFile(Path)} will be invoked with
     * path to new firmware, it is up to the implementation to install new firmware.
     *
     * <strong>Upon returning {@code true} application will be shut down,by firmware update module, with exit code 0.
     * It is up to the library integrator to provide 'daemon-ized' way of starting application in order to start application after
     * exit performed by firmware update procedure</strong>
     *
     * @param firmwareFile File which points to new firmware
     * @return {@code true} if successful, or {@code false} if firmware update failed
     */
    boolean updateFirmwareWithFile(Path firmwareFile);
}
