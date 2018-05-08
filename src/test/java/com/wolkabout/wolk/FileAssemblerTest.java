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
package com.wolkabout.wolk;

import com.wolkabout.wolk.filetransfer.FileAssembler;
import com.wolkabout.wolk.filetransfer.FileReceiver;
import com.wolkabout.wolk.filetransfer.FileTransferPacket;
import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static com.wolkabout.wolk.Utils.isFileValid;
import static com.wolkabout.wolk.Utils.joinByteArrays;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FileAssemblerTest {

    private static final String TEMP_FILE_PATH = "file-constructor-test.tmp";

    private File createTempFile() {
        return new File(TEMP_FILE_PATH);
    }

    @After
    public void tearDown() {
        final File tempFile = createTempFile();
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void Given_FileConstructor_When_FileConstructionIsStarted_Then_RequestForFirstPacketIsIssued() {
        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 3);
        constructor.initialize(TEMP_FILE_PATH, 1024, DigestUtils.sha256(new byte[]{}));

        // When
        final FileTransferPacketRequest request = constructor.packetRequest();

        // Then
        assertNotNull(request);
        assertEquals(TEMP_FILE_PATH, request.getFileName());
        assertEquals(0, request.getPacketId());
    }

    @Test
    public void Given_FileConstructorInitializedWithValidFileParameters_When_FileIsConstructedFromSinglePacket_Then_ConstructedFileIsValid() {
        final byte[] fileContent = "Hello world~!".getBytes(StandardCharsets.UTF_8);
        final byte[] fileSha256 = DigestUtils.sha256(fileContent);

        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 3);
        constructor.initialize(TEMP_FILE_PATH, fileContent.length, fileSha256);

        // When
        assertNotNull(constructor.packetRequest());

        final byte[] previousPacketHash = new byte[32];
        final byte[] fileChunk = fileContent.clone();
        final byte[] packetSha256 = DigestUtils.sha256(fileChunk);

        final FileAssembler.PacketProcessingError error = constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256)));
        assertEquals(FileAssembler.PacketProcessingError.NONE, error);

        // Then
        final File tempFile = createTempFile();
        assertTrue(tempFile.exists());
        assertTrue(isFileValid(tempFile, fileSha256));
    }

    @Test
    public void Given_FileConstructorWithNoRetriesPerPacket_When_InvalidPacketIsProcessed_Then_RetryCountExceededErrorIsReturned() {
        final byte[] fileContent = "Hello world~!".getBytes(StandardCharsets.UTF_8);
        final byte[] fileSha256 = DigestUtils.sha256(fileContent);

        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 1);
        constructor.initialize(TEMP_FILE_PATH, fileContent.length, fileSha256);

        // When
        assertNotNull(constructor.packetRequest());

        final byte[] previousPacketHash = new byte[32];
        final byte[] fileChunk = fileContent.clone();
        final byte[] packetSha256 = DigestUtils.sha256(joinByteArrays(fileChunk, "excessBytes".getBytes(StandardCharsets.UTF_8)));

        final FileAssembler.PacketProcessingError error = constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256)));

        // Then
        assertEquals(FileAssembler.PacketProcessingError.RETRY_COUNT_EXCEEDED, error);
    }

    @Test
    public void Given_FileConstructorWithRetriesPerPacket_When_InvalidPacketIsProcessed_Then_PacketIsRequestedAgain() {
        final byte[] fileContent = "Hello world~!".getBytes(StandardCharsets.UTF_8);
        final byte[] fileSha256 = DigestUtils.sha256(fileContent);

        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 3);
        constructor.initialize(TEMP_FILE_PATH, fileContent.length, fileSha256);

        // When
        final FileTransferPacketRequest initialRequest = constructor.packetRequest();

        final byte[] previousPacketHash = new byte[32];
        final byte[] fileChunk = fileContent.clone();
        final byte[] packetSha256 = DigestUtils.sha256(joinByteArrays(fileChunk, "excessBytes".getBytes(StandardCharsets.UTF_8)));

        final FileAssembler.PacketProcessingError error = constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256)));
        assertEquals(FileAssembler.PacketProcessingError.INVALID_CHECKSUM, error);

        final FileTransferPacketRequest retryRequest = constructor.packetRequest();
        assertNotNull(retryRequest);

        // Then
        assertEquals(initialRequest.getPacketId(), retryRequest.getPacketId());
        assertEquals(initialRequest.getPacketSize(), retryRequest.getPacketSize());
        assertEquals(initialRequest.getFileName(), retryRequest.getFileName());
    }

    @Test
    public void Given_FileConstructorInitializedWithValidFileParameters_When_FileIsConstructedFromSinglePacketWithRetry_Then_ConstructedFileIsValid() {
        final byte[] fileContent = "Hello world~!".getBytes(StandardCharsets.UTF_8);
        final byte[] fileSha256 = DigestUtils.sha256(fileContent);

        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 3);
        constructor.initialize(TEMP_FILE_PATH, fileContent.length, fileSha256);

        // When
        assertNotNull(constructor.packetRequest());

        byte[] previousPacketHash = new byte[32];

        // first packet first time
        byte[] fileChunk = fileContent.clone();
        byte[] packetSha256 = DigestUtils.sha256(joinByteArrays(fileChunk, "excessBytes".getBytes(StandardCharsets.UTF_8)));

        assertNotNull(constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256))));

        // first packet second time
        fileChunk = fileContent.clone();
        packetSha256 = DigestUtils.sha256(fileChunk);

        final FileAssembler.PacketProcessingError error = constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256)));
        assertEquals(FileAssembler.PacketProcessingError.NONE, error);

        assertNull(constructor.packetRequest());

        // Then
        final File tempFile = createTempFile();
        assertTrue(tempFile.exists());
        assertTrue(isFileValid(tempFile, fileSha256));
    }

    @Test
    public void Given_FileConstructorInitializedWithValidFileParameters_When_FileConstructionIsCompleted_Then_ListenerIsInvoked() {
        final byte[] fileContent = "Hello world~!".getBytes(StandardCharsets.UTF_8);
        final byte[] fileSha256 = DigestUtils.sha256(fileContent);

        final FileReceiver fileReceiver = mock(FileReceiver.class);

        // Given
        final FileAssembler constructor = new FileAssembler(new File(""), 3);
        constructor.initialize(TEMP_FILE_PATH, fileContent.length, fileSha256);
        constructor.setListener(fileReceiver);

        // When
        assertNotNull(constructor.packetRequest());

        byte[] previousPacketHash = new byte[32];

        // first packet first time
        byte[] fileChunk = fileContent.clone();
        byte[] packetSha256 = DigestUtils.sha256(joinByteArrays(fileChunk, "excessBytes".getBytes()));

        assertNotNull(constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256))));

        // first packet second time
        fileChunk = fileContent.clone();
        packetSha256 = DigestUtils.sha256(fileChunk);

        final FileAssembler.PacketProcessingError error = constructor.processPacket(new FileTransferPacket(joinByteArrays(previousPacketHash, fileChunk, packetSha256)));
        assertEquals(FileAssembler.PacketProcessingError.NONE, error);

        assertNull(constructor.packetRequest());

        // Then
        final File tempFile = createTempFile();
        assertTrue(tempFile.exists());
        assertTrue(isFileValid(tempFile, fileSha256));

        verify(fileReceiver).onFileReceived(tempFile);
    }
}
