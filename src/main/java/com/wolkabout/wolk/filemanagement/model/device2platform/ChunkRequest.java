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
package com.wolkabout.wolk.filemanagement.model.device2platform;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the payload sent by the device to the platform
 * to the `d2p/file_binary_request/d/` endpoint to receive binary file data.
 */
public class ChunkRequest {

    @JsonProperty("name")
    private String fileName;

    @JsonProperty("chunkIndex")
    private int chunkIndex;

    public ChunkRequest(String fileName, int chunkIndex) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    @Override
    public String toString() {
        return "ChunkRequest{" +
                "name='" + fileName + '\'' +
                ", chunkIndex=" + chunkIndex +
                '}';
    }
}
