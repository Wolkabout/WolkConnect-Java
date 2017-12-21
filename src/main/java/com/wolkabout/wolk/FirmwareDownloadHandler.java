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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Provide implementation of {@link FirmwareDownloadHandler} to enable firmware download from URL.
 */
public interface FirmwareDownloadHandler {
    /**
     * @param file URL of firmware file to be downloaded
     * @return {@code Path} to downloaded file
     * @throws IOException if {@code file} can not be obtained, or saved locally
     */
    Path downloadFile(URL file) throws IOException;
}
