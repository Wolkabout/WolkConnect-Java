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
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

public class FileDownloadSessionTest {

    private static Logger LOG = LoggerFactory.getLogger(FileDownloadSessionTest.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void nullCheckInitMessage() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The initial message object can not be null.");
        new FileDownloadSession(null, new FileDownloadSession.Callback() {
            @Override
            public void sendRequest(String fileName, int chunkIndex, int chunkSize) {

            }

            @Override
            public void onFinish(FileTransferStatus status, FileTransferError error) {

            }
        });
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
        final int CHUNK_EXTRA_SIZE = 64;
        final int START = 100;
        final int STEP = 100;
        final int MAX = 1000000 - CHUNK_EXTRA_SIZE;

        // Prepare the message
        FileInit message = new FileInit();
        message.setFileName("test-file.jar");
        message.setFileHash("test-file-hash");

        // Prepare the callback
        FileDownloadSession.Callback callback = new FileDownloadSession.Callback() {
            @Override
            public void sendRequest(String fileName, int chunkIndex, int chunkSize) {

            }

            @Override
            public void onFinish(FileTransferStatus status, FileTransferError error) {

            }
        };

        // Prepare the field for obtaining values
        Field chunkSizesField = FileDownloadSession.class.getDeclaredField("chunkSizes");
        chunkSizesField.setAccessible(true);

        // Do the looping around these values
        for (int i = START; i <= MAX; i += STEP) {
            // Adjust the message
            message.setFileSize(i);

            // Make the session
            FileDownloadSession session = new FileDownloadSession(message, callback);

            // Check the values
            List<Integer> chunkSizes = (List<Integer>) chunkSizesField.get(session);
            assertEquals(chunkSizes.size(), 1);
            assertEquals(chunkSizes.get(0), Integer.valueOf(i + CHUNK_EXTRA_SIZE));
        }
    }
}
