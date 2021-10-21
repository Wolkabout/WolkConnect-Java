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
package com.wolkabout.wolk.filemanagement.model.platform2device;

import java.util.List;

/**
 * This class represents the payload sent by the platform to the device
 * to the `p2d/file_delete/d/` endpoint to receive a new uploaded file.
 */
public class FileDelete {

    private List<String> fileNames;

    public FileDelete() {
    }

    public FileDelete(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    @Override
    public String toString() {
        return "FileDelete{" +
                "fileNames='" + fileNames + '\'' + '}';
    }
}
