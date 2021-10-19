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
package com.wolkabout.wolk.firmwareupdate.model.platform2device;

import java.util.Arrays;

/**
 * This class represents the payload sent by the platform to the device
 * to the `p2d/firmware_update_install/d/` endpoint to start a firmware install.
 */
public class UpdateInit {

    private String fileName;

    public UpdateInit() {
    }

    public UpdateInit(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "UpdateInit{" +
                "fileName='" + fileName + '\'' +
                '}';
    }
}
