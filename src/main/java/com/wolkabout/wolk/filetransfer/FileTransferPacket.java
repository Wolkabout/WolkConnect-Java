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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FileTransferPacket {
    private static final Logger LOG = LoggerFactory.getLogger(FileTransferPacket.class);

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

        if (bytes.length <= 64) {
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
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(previousPacketHash);
            md.update(data);

            return Arrays.equals(md.digest(), hash);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Unable to validate packet integrity", e);
            return false;
        }
    }
}
