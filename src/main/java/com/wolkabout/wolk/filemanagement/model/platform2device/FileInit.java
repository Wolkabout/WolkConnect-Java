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
package com.wolkabout.wolk.filemanagement.model.platform2device;

/**
 * This class represents the payload sent by the platform to the device
 * to the `file_upload_initiate` endpoint to receive a new uploaded file.
 */
public class FileInit {
    private String name;
    private long size;
    private String hash;

    public String getFileName() {
        return name;
    }

    public long getFileSize() {
        return size;
    }

    public String getFileHash() {
        return hash;
    }

    public void setFileName(String name) {
        this.name = name;
    }

    public void setFileSize(long size) {
        this.size = size;
    }

    public void setFileHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "FileInit{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", hash='" + hash + '\'' +
                '}';
    }
}
