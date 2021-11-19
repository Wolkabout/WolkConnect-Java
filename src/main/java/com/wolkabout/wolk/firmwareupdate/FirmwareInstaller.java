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
package com.wolkabout.wolk.firmwareupdate;

import org.apache.commons.lang3.StringUtils;

public interface FirmwareInstaller {
    boolean onInstallCommandReceived(String fileName);

    void onAbortCommandReceived();

    String getFirmwareVersion();

    /**
     * Checks if new firmware version exists at provided url
     *
     * @param url
     * @return
     */
    default boolean isNewVersionAvailable(String url) {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Url cannot be empty");
        }

        return true;
    }
}
