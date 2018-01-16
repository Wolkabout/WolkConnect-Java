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
package com.wolkabout.wolk.filetransfer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class FileTransferPacketRequest {
    private final String fileName;
    private final long packetId;
    private final long packetSize;

    public FileTransferPacketRequest(final String fileName, long packetId, long packetSize) {
        this.fileName = fileName;
        this.packetId = packetId;
        this.packetSize = packetSize;
    }

    @JsonProperty(value = "fileName")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty(value = "chunkIndex")
    public long getPacketId() {
        return packetId;
    }

    @JsonProperty(value = "chunkSize")
    public long getPacketSize() {
        return packetSize;
    }

    @Override
    public String toString() {
        return "FileTransferPacketRequest{" +
                "fileName='" + fileName + '\'' +
                ", packetId=" + packetId +
                ", packetSize=" + packetSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTransferPacketRequest that = (FileTransferPacketRequest) o;
        return packetId == that.packetId &&
                packetSize == that.packetSize &&
                Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, packetId, packetSize);
    }
}