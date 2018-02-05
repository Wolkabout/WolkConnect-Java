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
package com.wolkabout.wolk.filetransfer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;

public class FileTransferPacket {
    private static final int MINIMUM_PACKET_SIZE = 65; // 64 bytes of SHA-256 hash, and 1 byte of data

    private final byte[] hash;
    private final byte[] previousPacketHash;

    private final byte[] data;

    public FileTransferPacket(final byte[] bytes) throws IllegalArgumentException {
        /*
         Packet structure:
         Part 1:   32 bytes   - SHA-256 Hash of previous packet
         Part 2: >= 1 byte(s) - File chunk
         Part 3:   32 bytes   - SHA-256 hash of concatenated Part 1 and Part 2
         */

        if (bytes.length < MINIMUM_PACKET_SIZE) {
            throw new IllegalArgumentException("Invalid packet.");
        }

        previousPacketHash = Arrays.copyOfRange(bytes, 0, 32);

        data = Arrays.copyOfRange(bytes, 32, bytes.length - 32);

        hash = Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length);
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getPreviousPacketHash() {
        return previousPacketHash;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isChecksumValid() {
        final byte[] dataSha256 = DigestUtils.sha256(data);
        return Arrays.equals(dataSha256, hash);
    }

    @Override
    public String toString() {
        return "FileTransferPacket{" +
                "previousPacketHash=" + Base64.encodeBase64String(previousPacketHash) +
                ", hash=" + Base64.encodeBase64String(hash) +
                '}';
    }
}
