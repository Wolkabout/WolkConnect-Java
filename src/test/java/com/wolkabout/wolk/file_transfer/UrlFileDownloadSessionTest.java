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

import com.wolkabout.wolk.filemanagement.UrlFileDownloadSession;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.device2platform.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UrlFileDownloadSessionTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSessionTest.class);

    private final UrlInfo testMessage;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    UrlFileDownloadSession.Callback callbackMock;
    private UrlFileDownloadSession session;

    public UrlFileDownloadSessionTest() {
        testMessage = new UrlInfo();
        testMessage.setFileUrl("https://get.docker.com");
    }

    @Test
    public void nullCheckInitMessage() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The initial message object can not be null.");
        new UrlFileDownloadSession(null, callbackMock);
    }

    @Test
    public void nullCheckCallback() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The callback object can not be null.");
        new UrlFileDownloadSession(new UrlInfo(), null);
    }

    @Test
    public void checkInitialMessageIntegrity() {
        // Make the session
        session = new UrlFileDownloadSession(testMessage, callbackMock);

        // Check everything
        assertEquals(testMessage.getFileUrl(), session.getInitMessage().getFileUrl());
    }

    @Test
    public void simpleMalformedUrl() throws InterruptedException {
        // Make the invalid URL message
        UrlInfo invalidMessage = new UrlInfo();
        invalidMessage.setFileUrl("a.b@b.a");

        // Make the session
        session = new UrlFileDownloadSession(invalidMessage, callbackMock);

        // Wait for the calls to be done
        Thread.sleep(1000);

        // Check that there is no data
        assertEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.ERROR);
        assertEquals(session.getError(), FileTransferError.MALFORMED_URL);
        assertFalse(session.isSuccess());

        // Check that the callback was called
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ERROR, FileTransferError.MALFORMED_URL);
    }

    @Test
    public void simpleInvalidUrl() throws InterruptedException {
        // Make the invalid URL message
        UrlInfo invalidMessage = new UrlInfo();
        invalidMessage.setFileUrl("https://asdf.asdf");

        // Make the session
        session = new UrlFileDownloadSession(invalidMessage, callbackMock);

        // Wait for the calls to be done
        Thread.sleep(1000);

        // Check that there is no data
        assertEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.ERROR);
        assertEquals(session.getError(), FileTransferError.UNSPECIFIED_ERROR);
        assertFalse(session.isSuccess());

        // Check that the callback was called
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ERROR, FileTransferError.UNSPECIFIED_ERROR);
    }

    @Test
    public void simpleScriptHappyFlow() throws InterruptedException {
        // Make the session
        session = new UrlFileDownloadSession(testMessage, callbackMock);

        // Check that it is running
        assertTrue(session.isRunning());

        // Wait for the call to be here
        Thread.sleep(1000);

        // Check that there is some data
        assertNotEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.FILE_READY);
        assertNull(session.getError());
        assertEquals(session.getFileName(), "get.docker.com");
        assertTrue(session.isSuccess());

        // Verify that the mock was called with the proper result
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void simpleAbort() throws InterruptedException {
        // Make the session
        session = new UrlFileDownloadSession(testMessage, callbackMock);

        // Abort the session
        assertTrue(session.abort());

        // Wait for the call to be done
        Thread.sleep(1000);

        // Check the data and state
        assertEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.ABORTED);
        assertNull(session.getError());
        assertTrue(session.isAborted());

        // Verify the mock call
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ABORTED, null);
    }

    @Test
    public void abortAfterSuccess() throws InterruptedException {
        // Make the session
        session = new UrlFileDownloadSession(testMessage, callbackMock);

        // Check that it is running
        assertTrue(session.isRunning());

        // Wait for the call to be here
        Thread.sleep(1000);

        // Check that there is some data
        assertNotEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.FILE_READY);
        assertNull(session.getError());
        assertEquals(session.getFileName(), "get.docker.com");
        assertTrue(session.isSuccess());

        // Attempt to abort
        assertFalse(session.abort());

        // Verify that the mock was called with the proper result
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void abortAfterAbort() throws InterruptedException {
        // Make the session
        session = new UrlFileDownloadSession(testMessage, callbackMock);

        // Abort the session
        assertTrue(session.abort());

        // Wait for the call to be done
        Thread.sleep(1000);

        // Check the data and state
        assertEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.ABORTED);
        assertNull(session.getError());
        assertTrue(session.isAborted());

        // Attempt to abort again
        assertFalse(session.abort());

        // Verify the mock call
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ABORTED, null);
    }

    @Test
    public void abortAfterError() throws InterruptedException {
        // Make the invalid URL message
        UrlInfo invalidMessage = new UrlInfo();
        invalidMessage.setFileUrl("https://asdf.asdf");

        // Make the session
        session = new UrlFileDownloadSession(invalidMessage, callbackMock);

        // Wait for the calls to be done
        Thread.sleep(1000);

        // Check that there is no data
        assertEquals(session.getFileData().length, 0);
        assertEquals(session.getStatus(), FileTransferStatus.ERROR);
        assertEquals(session.getError(), FileTransferError.UNSPECIFIED_ERROR);
        assertFalse(session.isSuccess());

        // Attempt to abort again
        assertFalse(session.abort());

        // Check that the callback was called
        verify(callbackMock, times(1)).onFinish(FileTransferStatus.ERROR, FileTransferError.UNSPECIFIED_ERROR);
    }
}
