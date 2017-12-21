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

import com.wolkabout.wolk.filetransfer.FileTransferPacket;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static com.wolkabout.wolk.Utils.calculateSha256;
import static com.wolkabout.wolk.Utils.joinByteArrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileTransferPacketTest {

    @Test
    public void Given_ValidPacketBytes_When_PacketIsConstructed_Then_PacketContainsValidData() {
        // Given
        final byte[] previousPacketHash = calculateSha256("".getBytes(StandardCharsets.UTF_8));
        final byte[] data = "Hello world".getBytes();
        final byte[] hash = calculateSha256(data);

        final byte[] packetBytes = joinByteArrays(previousPacketHash, data, hash);

        // When
        final FileTransferPacket packet = new FileTransferPacket(packetBytes);

        // Then
        assertArrayEquals(previousPacketHash, packet.getPreviousPacketHash());
        assertArrayEquals(data, packet.getData());
        assertArrayEquals(hash, packet.getHash());
    }

    @Test
    public void Given_ValidPacketBytes_When_PacketIsConstructed_Then_PacketHashIsValid() {
        // Given
        final byte[] previousPacketHash = calculateSha256("".getBytes(StandardCharsets.UTF_8));
        final byte[] data = "Hello world".getBytes();
        final byte[] hash = calculateSha256(joinByteArrays(previousPacketHash, data));

        final byte[] packetBytes = joinByteArrays(previousPacketHash, data, hash);

        // When
        final FileTransferPacket packet = new FileTransferPacket(packetBytes);

        // Then
        assertTrue(packet.isChecksumValid());
    }

    @Test
    public void Given_CorruptedPacketBytes_When_PacketIsConstructed_Then_PacketHashIsNotValid() {
        // Given
        final byte[] previousPacketHash = calculateSha256("".getBytes(StandardCharsets.UTF_8));
        final byte[] data = "Hello wo".getBytes();
        final byte[] hash = calculateSha256("Hello world".getBytes());

        final byte[] packetBytes = joinByteArrays(previousPacketHash, data, hash);

        // When
        final FileTransferPacket packet = new FileTransferPacket(packetBytes);

        // Then
        assertFalse(packet.isChecksumValid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void Given_PartialPacketBytes_When_PacketIsConstructed_Then_IllegalArgumentExceptionIsThrown() {
        // Given
        final byte[] previousPacketHash = calculateSha256("".getBytes(StandardCharsets.UTF_8));
        final byte[] data = "Hello world".getBytes();
        final byte[] hash = "partialPacketHash".getBytes();

        final byte[] packetBytes = joinByteArrays(previousPacketHash, data, hash);

        // When
        final FileTransferPacket packet = new FileTransferPacket(packetBytes);

        // Then
        // IllegalArgumentException is thrown
    }
}
