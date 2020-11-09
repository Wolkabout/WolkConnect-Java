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
 * to the `d2p/file_binary_request/d/` endpoint to receive binary file data.
 */
public class ChunkRequest {

    private String fileName;
    private int chunkIndex;
    private int chunkSize;

    public ChunkRequest(String fileName, int chunkIndex, int chunkSize) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
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

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public String toString() {
        return "ChunkRequest{" +
                "fileName='" + fileName + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", chunkSize=" + chunkSize +
                '}';
    }
}
