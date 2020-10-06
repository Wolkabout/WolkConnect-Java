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
package com.wolkabout.wolk.file_transfer;

import com.wolkabout.wolk.filemanagement.FileDownloadSession;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadSessionTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSessionTest.class);

    private final int CHUNK_EXTRA = 64;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    FileDownloadSession.Callback callbackMock;

    @Test
    public void nullCheckInitMessage() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The initial message object can not be null.");
        new FileDownloadSession(null, callbackMock);
    }

    @Test
    public void nullCheckCallback() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The callback object can not be null.");
        new FileDownloadSession(new FileInit(), null);
    }

    @Test
    public void chunkSizeOneChunk() throws NoSuchFieldException, IllegalAccessException {
        // Prepare the steps
        final int START = 100;
        final int STEP = 100;
        final int MAX = 1000000 - CHUNK_EXTRA;

        // Prepare the message
        FileInit message = new FileInit();
        message.setFileName("test-file.jar");
        message.setFileHash("test-file-hash");

        // Prepare the field for obtaining values
        Field chunkSizesField = FileDownloadSession.class.getDeclaredField("chunkSizes");
        chunkSizesField.setAccessible(true);

        // Do the looping around these values
        for (int i = START; i <= MAX; i += STEP) {
            // Adjust the message
            message.setFileSize(i);

            // Make the session
            FileDownloadSession session = new FileDownloadSession(message, callbackMock);

            // Check the values
            List<Integer> chunkSizes = (List<Integer>) chunkSizesField.get(session);
            assertEquals(chunkSizes.size(), 1);
            assertEquals(chunkSizes.get(0), Integer.valueOf(i + CHUNK_EXTRA));
        }
    }

    @Test
    public void singleChunkReceiveAll() {
        // Define the constants
        final int fileSize = 1024;

        // Create the message
        FileInit initial = new FileInit();
        initial.setFileName("test-file.jar");
        initial.setFileHash("test-file-hash");
        initial.setFileSize(fileSize);

        // Create the session
        FileDownloadSession session = new FileDownloadSession(initial, callbackMock);

        // Give the session all the bytes
        assertTrue(session.receiveBytes(new byte[fileSize + CHUNK_EXTRA]));

        // Verify that the mock was called
        verify(callbackMock, atLeastOnce()).sendRequest(anyString(), anyInt(), anyInt());
        verify(callbackMock, atLeastOnce()).onFinish(any(), any());
    }
}
