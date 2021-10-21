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
package com.wolkabout.wolk.filemanagement;

import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadSessionTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSessionTest.class);

    private final int MAX_CHUNK_SIZE = 255936;
    private final int CHUNK_EXTRA = 64;
    private final int MAX_MESSAGE_SIZE = (MAX_CHUNK_SIZE + CHUNK_EXTRA) / 1024;
    // Define the constants
    private final int testFileSize = 1024;
    private final byte[] testMessageFileHash;
    private final byte[] testMessageHash;
    private final FileInit testMessage;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    FileDownloadSession.Callback callbackMock;
    private FileDownloadSession session;

    public FileDownloadSessionTest() {
        testMessage = new FileInit();
        testMessage.setFileName("test-file.jar");
        byte[] actualBytes = new byte[testFileSize];
        testMessageFileHash = FileDownloadSession.calculateMD5HashForBytes(actualBytes);
        testMessageHash = DigestUtils.sha256(actualBytes);
        testMessage.setFileHash(DatatypeConverter.printHexBinary(testMessageFileHash));
        testMessage.setFileSize(testFileSize);
    }

    @Test
    public void nullCheckInitMessage() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The initial message object can not be null.");
        new FileDownloadSession(null, callbackMock, 0);
    }

    @Test
    public void nullCheckCallback() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The callback object can not be null.");
        new FileDownloadSession(new FileInit(), null, 0);
    }

    @Test
    public void checkInitialMessageIntegrity() {
        // Make the session
        session = new FileDownloadSession(testMessage, callbackMock, 0);

        // Check everything
        assertEquals(testMessage.getFileName(), session.getInitMessage().getFileName());
        assertEquals(testMessage.getFileHash(), session.getInitMessage().getFileHash());
        assertEquals(testMessage.getFileSize(), session.getInitMessage().getFileSize());
    }

    @Test
    public void chunkSizeOneChunk() throws NoSuchFieldException, IllegalAccessException {
        // Prepare the steps
        final int START = 100;
        final int STEP = 100;
        final int MAX = MAX_CHUNK_SIZE;

        // Prepare the message
        FileInit message = new FileInit();
        message.setFileName("test-file.jar");
        message.setFileHash("test-hash");

        // Prepare the field for obtaining values
        Field chunkSizesField = FileDownloadSession.class.getDeclaredField("chunkSizes");
        chunkSizesField.setAccessible(true);

        // Do the looping around these values
        for (int i = START; i <= MAX; i += STEP) {
            // Adjust the message
            message.setFileSize(i);

            // Make the session
            session = new FileDownloadSession(message, callbackMock, MAX_MESSAGE_SIZE);

            // Check the values
            List<Long> chunkSizes = (List<Long>) chunkSizesField.get(session);
            assertEquals(chunkSizes.size(), 1);
            assertEquals(chunkSizes.get(0), Long.valueOf(i + CHUNK_EXTRA));
        }
    }

    @Test
    public void chunkSizeMultipleChunks() throws NoSuchFieldException, IllegalAccessException {
        // Prepare the steps
        final int START = 1000;
        final int MAX = 15000000;

        // Prepare the message
        FileInit message = new FileInit();
        message.setFileName("test-file.jar");
        message.setFileHash("test-hash");

        // Prepare the field for obtaining values
        Field chunkSizesField = FileDownloadSession.class.getDeclaredField("chunkSizes");
        chunkSizesField.setAccessible(true);

        // Do the looping around these values
        for (int i = START; i <= MAX; i += MAX_CHUNK_SIZE) {
            // Adjust the message
            message.setFileSize(i);

            // Make the session
            session = new FileDownloadSession(message, callbackMock, MAX_MESSAGE_SIZE);

            // Check the values
            List<Long> chunkSizes = (List<Long>) chunkSizesField.get(session);
            assertEquals(chunkSizes.size(), (i / MAX_CHUNK_SIZE) + 1);
            for (int j = 0; j < (i / (MAX_CHUNK_SIZE + CHUNK_EXTRA)) + 1; j++) {
                if (j == (i / MAX_CHUNK_SIZE)) {
                    assertEquals(chunkSizes.get(j), Long.valueOf((i + CHUNK_EXTRA) % MAX_CHUNK_SIZE));
                } else {
                    assertEquals(chunkSizes.get(j), Long.valueOf(MAX_CHUNK_SIZE + CHUNK_EXTRA));
                }
            }
        }
    }

    @Test
    public void singleChunkReceiveAll() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that it is reporting that it is running
        assertEquals(FileTransferStatus.FILE_TRANSFER, session.getStatus());

        // Form the payload
        byte[] payload = new byte[testFileSize + CHUNK_EXTRA];
        for (int i = 0; i < testMessageHash.length; i++) {
            payload[(CHUNK_EXTRA / 2) + testFileSize + i] = testMessageHash[i];
        }

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Give the session all the bytes
        assertTrue(session.receiveBytes(payload));

        // Verify that the payload is all zeroes
        for (byte value : session.getBytes()) {
            assertEquals(value, 0);
        }

        // Check if it says it is successful
        assertEquals(FileTransferStatus.FILE_READY, session.getStatus());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void singleChunkSimpleAbort() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // It is still not aborted
        assertEquals(FileTransferStatus.FILE_TRANSFER, session.getStatus());

        // Abort the transfer
        assertTrue(session.abort());

        // Check that it says true
        assertEquals(FileTransferStatus.ABORTED, session.getStatus());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ABORTED, null);
    }

    @Test
    public void singleChunkAbortAfterSuccess() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Form the payload
        byte[] payload = new byte[testFileSize + CHUNK_EXTRA];
        for (int i = 0; i < testMessageHash.length; i++) {
            payload[(CHUNK_EXTRA / 2) + testFileSize + i] = testMessageHash[i];
        }

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Give the session all the bytes
        assertTrue(session.receiveBytes(payload));

        // Verify that the payload is all zeroes
        for (byte value : session.getBytes()) {
            assertEquals(value, 0);
        }

        // Try to abort now that it is successful
        assertFalse(session.abort());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void singleChunkAbortAfterAbort() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Abort the transfer
        assertTrue(session.abort());

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.ABORTED);
        assertNull(session.getError());

        // This abort should not work
        assertFalse(session.abort());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ABORTED, null);
    }

    @Test
    public void singleChunkReceiveDataAfterAbort() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Abort the transfer
        assertTrue(session.abort());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ABORTED, null);

        // Expect an exception to be thrown for data
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("This session is not running anymore, it is not accepting chunks.");
        session.receiveBytes(new byte[1]);
    }

    @Test
    public void singleChunkNotEnoughBytes() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);

        // Expect an exception to be thrown for data
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The passed bytes is not a valid chunk message.");
        session.receiveBytes(new byte[1]);
    }

    @Test
    public void singleChunkWrongBytes() throws InterruptedException {
        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Check that the status is FILE_TRANSFER
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Verify that the mock was called
        verify(callbackMock, times(1)).sendRequest("test-file.jar", 0);

        // Expect an exception to be thrown for data
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The passed bytes is not the same size as requested.");
        session.receiveBytes(new byte[80]);
    }

    @Test
    public void singleChunkInvalidBeforeHash() throws InterruptedException {
        // Setup the false byte array
        byte[] bytes = new byte[testFileSize + CHUNK_EXTRA];
        bytes[0] = 127;

        // Setup the answer from mock
        doAnswer(invocation -> {
            if (session != null)
                session.receiveBytes(bytes);
            return null;
        }).when(callbackMock).sendRequest(testMessage.getFileName(), 0);

        // Create the session
        session = new FileDownloadSession(testMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Sleep a tad bit for the mocks to be called
        Thread.sleep(100);

        // Attempt to abort, but it is already not running. This is an abort after error.
        assertFalse(session.abort());

        // Verify that the mock was called
        verify(callbackMock, times(4)).sendRequest("test-file.jar", 0);
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ERROR, FileTransferError.RETRY_COUNT_EXCEEDED);
    }

    @Test
    public void singleChunkInvalidFileHash() throws InterruptedException {
        // Prepare an invalid initialMessage
        FileInit initialMessage = new FileInit();
        initialMessage.setFileName("test-file.jar");
        initialMessage.setFileHash(DatatypeConverter.printHexBinary(FileDownloadSession.calculateMD5HashForBytes(new byte[1000])));
        initialMessage.setFileSize(testFileSize);

        // Prepare the payload
        byte[] hash = DigestUtils.sha256(new byte[testFileSize]);
        byte[] payload = new byte[testFileSize + CHUNK_EXTRA];
        System.arraycopy(hash, 0, payload, payload.length - 32, hash.length);

        // Setup the return
        doAnswer(invocation -> {
            session.receiveBytes(payload);
            return null;
        }).when(callbackMock).sendRequest(anyString(), anyInt());

        // Setup the session
        session = new FileDownloadSession(initialMessage, callbackMock, MAX_MESSAGE_SIZE);
        // Sleep for a bit
        Thread.sleep(1000);

        // Check that we received the expected output
        verify(callbackMock, times(4)).sendRequest(anyString(), anyInt());
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ERROR, FileTransferError.RETRY_COUNT_EXCEEDED);
    }

    @Test
    public void multipleChunkHappyFlow() throws InterruptedException {
        // This is a test case where we are going to emulate transfer of a 5MB file, where the data are all zeroes.
        // This is going to be 6 chunks, hashes are all going to be the same ones.

        // Calculate the hash
        byte[] hash = DigestUtils.sha256(new byte[MAX_CHUNK_SIZE]);
        // Setup the chunks
        byte[] firstChunk = new byte[MAX_CHUNK_SIZE + CHUNK_EXTRA], otherChunks = new byte[MAX_CHUNK_SIZE + CHUNK_EXTRA];
        for (int i = 0; i < hash.length; i++) {
            // Copy into first bytes for other chunks
            otherChunks[i] = hash[i];
            // Copy into last bytes for other chunks
            firstChunk[firstChunk.length - (CHUNK_EXTRA / 2) + i] = hash[i];
            otherChunks[otherChunks.length - (CHUNK_EXTRA / 2) + i] = hash[i];
        }

        // Prepare the message
        byte[] messageHash = DigestUtils.sha256(new byte[5 * MAX_CHUNK_SIZE]);
        FileInit message = new FileInit();
        message.setFileName("test-file-message");
        message.setFileHash(DatatypeConverter.printHexBinary(FileDownloadSession.calculateMD5HashForBytes(new byte[5 * MAX_CHUNK_SIZE])));
        message.setFileSize(5 * MAX_CHUNK_SIZE);

        // Prepare the queue
        Queue<byte[]> queue = new LinkedList<byte[]>() {{
            add(firstChunk);
            add(otherChunks);
            add(otherChunks);
            add(otherChunks);
            add(otherChunks);
        }};

        // Setup the calls
        doAnswer(invocation -> {
            session.receiveBytes(Objects.requireNonNull(queue.poll()));
            return null;
        }).when(callbackMock).sendRequest(anyString(), anyInt());

        // Trigger the calls
        session = new FileDownloadSession(message, callbackMock, MAX_MESSAGE_SIZE);

        // Check that it is running
        assertEquals(session.getStatus(), FileTransferStatus.FILE_TRANSFER);
        assertNull(session.getError());

        // Wait for the calls to execute
        Thread.sleep(1000);

        // Verify everything was called, and the status was returned successfully.
        verify(callbackMock, times(5)).sendRequest(anyString(), anyInt());
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void multiChunkSecondChunkInvalidHashFirstTime() throws InterruptedException {
        // Calculate the hashes
        byte[] firstHash = DigestUtils.sha256(new byte[MAX_CHUNK_SIZE]);
        byte[] secondHash = DigestUtils.sha256(new byte[MAX_CHUNK_SIZE / 2]);

        // Create the payload
        byte[] firstPayload = new byte[MAX_CHUNK_SIZE + CHUNK_EXTRA];
        byte[] secondPayloadInvalid = new byte[(MAX_CHUNK_SIZE / 2) + CHUNK_EXTRA];
        byte[] secondPayloadValid = new byte[(MAX_CHUNK_SIZE / 2) + CHUNK_EXTRA];
        for (int i = 0; i < firstHash.length; i++) {
            // Set the first hash as valid
            firstPayload[firstPayload.length - (CHUNK_EXTRA / 2) + i] = firstHash[i];
            secondPayloadInvalid[i] = firstHash[i];
            secondPayloadValid[i] = firstHash[i];
            // Set the second hash as valid
            secondPayloadValid[secondPayloadValid.length - (CHUNK_EXTRA / 2) + i] = secondHash[i];
        }

        // Prepare the initial message
        FileInit initialMessage = new FileInit();
        initialMessage.setFileName("test-file.jar");
        initialMessage.setFileHash(DatatypeConverter.printHexBinary(
                FileDownloadSession.calculateMD5HashForBytes(new byte[(MAX_CHUNK_SIZE + (MAX_CHUNK_SIZE / 2))])));
        initialMessage.setFileSize(MAX_CHUNK_SIZE + (MAX_CHUNK_SIZE / 2));

        // Prepare the message goings
        Queue<byte[]> queue = new LinkedList<byte[]>() {{
            add(firstPayload);
            add(secondPayloadInvalid);
            add(secondPayloadValid);
        }};
        doAnswer(invocation -> {
            session.receiveBytes(Objects.requireNonNull(queue.poll()));
            return null;
        }).when(callbackMock).sendRequest(anyString(), anyInt());

        // Prepare the session and run everything
        session = new FileDownloadSession(initialMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Sleep for a bit
        Thread.sleep(1000);

        // Verify all the calls
        verify(callbackMock, times(3)).sendRequest(anyString(), anyInt());
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void multiChunkRewindBack() throws InterruptedException {
        // Calculate the hashes
        byte[] firstHash = DigestUtils.sha256(new byte[MAX_CHUNK_SIZE]);
        byte[] secondHash = DigestUtils.sha256(new byte[MAX_CHUNK_SIZE / 2]);

        // Create the payload
        byte[] firstPayloadInvalid = new byte[MAX_CHUNK_SIZE + CHUNK_EXTRA];
        byte[] firstPayloadValid = new byte[MAX_CHUNK_SIZE + CHUNK_EXTRA];
        byte[] secondPayloadInvalid = new byte[(MAX_CHUNK_SIZE / 2) + CHUNK_EXTRA];
        byte[] secondPayloadValid = new byte[(MAX_CHUNK_SIZE / 2) + CHUNK_EXTRA];
        for (int i = 0; i < firstHash.length; i++) {
            // Put the first hash where it needs to be put in
            firstPayloadInvalid[firstPayloadInvalid.length - (CHUNK_EXTRA / 2) + i] = firstHash[i];
            firstPayloadValid[firstPayloadValid.length - (CHUNK_EXTRA / 2) + i] = firstHash[i];
            secondPayloadValid[i] = firstHash[i];
            // Put the second hash where it needs to be put in
            secondPayloadInvalid[secondPayloadInvalid.length - (CHUNK_EXTRA / 2) + i] = secondHash[i];
            secondPayloadValid[secondPayloadValid.length - (CHUNK_EXTRA / 2) + i] = secondHash[i];
        }

        // Prepare the initial message
        FileInit initialMessage = new FileInit();
        initialMessage.setFileName("test-file.jar");
        initialMessage.setFileHash(DatatypeConverter.printHexBinary(
                FileDownloadSession.calculateMD5HashForBytes(new byte[(MAX_CHUNK_SIZE + (MAX_CHUNK_SIZE / 2))])));
        initialMessage.setFileSize(MAX_CHUNK_SIZE + (MAX_CHUNK_SIZE / 2));

        // Prepare the message goings
        Queue<byte[]> queue = new LinkedList<byte[]>() {{
            add(firstPayloadInvalid);
            add(secondPayloadInvalid);
            add(firstPayloadValid);
            add(secondPayloadValid);
        }};
        doAnswer(invocation -> {
            session.receiveBytes(Objects.requireNonNull(queue.poll()));
            return null;
        }).when(callbackMock).sendRequest(anyString(), anyInt());

        // Prepare the session and run everything
        session = new FileDownloadSession(initialMessage, callbackMock, MAX_MESSAGE_SIZE);

        // Sleep for a bit
        Thread.sleep(1000);

        // Verify all the calls
        verify(callbackMock, times(4)).sendRequest(anyString(), anyInt());
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }
}
