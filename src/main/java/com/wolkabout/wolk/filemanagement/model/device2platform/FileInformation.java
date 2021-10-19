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
package com.wolkabout.wolk.filemanagement.model.device2platform;

/**
 * This class represents the payload sent by the device to the platform
 * used in arrays to be send to 'd2p/file_list_response/d/' or 'd2p/file_list_update/d/'.
 */
public class FileInformation {
    private String name;
    private long size;
    private String hash;

    public FileInformation(String name, long size, String hash) {
        this.name = name;
        this.size = size;
        this.hash = hash;
    }

    public String getFileName() {
        return name;
    }

    public void setFileName(String fileName) {
        this.name = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "FileInformation={" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", hash='" + hash + '\'' +
                '}';
    }
}
