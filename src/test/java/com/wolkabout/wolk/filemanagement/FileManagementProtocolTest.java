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
import com.wolkabout.wolk.filemanagement.model.platform2device.FileAbort;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileDelete;
import com.wolkabout.wolk.filemanagement.model.platform2device.FileInit;
import com.wolkabout.wolk.filemanagement.model.platform2device.UrlInfo;
import com.wolkabout.wolk.util.JsonUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileManagementProtocolTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocolTest.class);
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    File fileMock;
    @Mock
    FileDownloadSession fileDownloadSessionMock;
    @Mock
    UrlFileDownloadSession urlFileDownloadSessionMock;
    @Mock
    MqttClient clientMock;
    @Mock
    FileSystemManagement managementMock;
    FileManagementProtocol protocol;

    @Test
    public void nullCheckMqttClient() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The client cannot be null.");
        new FileManagementProtocol(null, managementMock);
    }

    @Test
    public void nullCheckFileManagement() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("The file management cannot be null.");
        new FileManagementProtocol(clientMock, null);
    }

    @Test
    public void createDefaultPath() {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Check that everything is valid
        assertEquals(managementMock, protocol.management);
        assertEquals(clientMock, protocol.client);
    }

    @Test
    public void subscribeThrows() throws MqttException {
        // Setup the throwing
        doThrow(new MqttException(new Exception("Test MQTT exception."))).when(clientMock)
                .subscribe(anyString(), anyInt(), any());

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);
        protocol.subscribe();

        // Verify the mock calls
        verify(clientMock, times(2)).getClientId();
        verify(clientMock, times(1)).subscribe(anyString(), anyInt(), any());
    }

    @Test
    public void subscribeTests() throws MqttException {
        // In here, we must check that the protocol will subscribe to each and every topic
        String clientId = "test-client-id";
        doReturn(clientId).when(clientMock).getClientId();
        final int requiredTopics = 8;

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);
        protocol.subscribe();

        // Verify all they belong
        verify(clientMock, times(requiredTopics))
                .subscribe(anyString(), anyInt(), any());
        verify(clientMock, times(requiredTopics * 2)).getClientId();
    }

    @Test
    public void listAllFilesException() throws IOException {
        // Setup the throw from management
        doThrow(new IOException("Test IO Exception throw.")).when(managementMock).listAllFiles();

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Expect the throws
        protocol.publishFileList();

        // Verify all the mock calls
        verify(clientMock, times(0)).getClientId();
    }

    @Test
    public void publishThrows() throws MqttException, IOException {
        // Setup the throw from management
        doThrow(new MqttException(new Exception("Test MQTT Exception throw."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.publishFileList();

        // Verify the mock calls
        verify(managementMock, times(1)).listAllFiles();
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void publishFileListTest() throws MqttException, IOException {
        // Setup return from management
        doReturn(new ArrayList<String>() {{
            add("File1");
            add("File2");
        }}).when(managementMock).listAllFiles();

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.publishFileList();

        // Verify all the mock calls
        verify(managementMock, times(1)).listAllFiles();
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void initializationMessagePublishThrows() throws MqttException {
        // Setup the mock calls
        when(managementMock.getFile(anyString()))
                .thenReturn(null);
        doThrow(new MqttException(new Exception("Test MQTT Exception."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Call the method
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);
    }

    @Test
    public void initializationMessageSessionsAreRunning() throws NoSuchFieldException, IllegalAccessException, MqttException {
        // Create the init message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-init-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Setup the values
        Field sessionField = FileManagementProtocol.class.getDeclaredField("fileDownloadSession");
        sessionField.setAccessible(true);
        sessionField.set(protocol, new FileDownloadSession(testInitMessage, new FileDownloadSession.Callback() {
            @Override
            public void sendRequest(String fileName, int chunkIndex) {

            }

            @Override
            public void onFinish(FileTransferStatus status, FileTransferError error) {

            }
        }, protocol.maxChunkSize));

        // Create the mqtt message, and call the initialization
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);

        // Verify no mock has been called
        verify(managementMock, never()).getFile(anyString());
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void initializationMessageNoManagement() throws MqttException, NoSuchFieldException, IllegalAccessException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Setup the values
        Field sessionField = FileManagementProtocol.class.getDeclaredField("management");
        sessionField.setAccessible(true);
        sessionField.set(protocol, null);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Call the method
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);

        // Verify all the mock calls
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void initializationMessageFileExistsAndHashesMatch() throws IOException, MqttException {
        // Create the bogus file
        char[] chars = new char[]{100, 100, 100, 100, 100};
        FileWriter writer = new FileWriter("test-bogus-file");
        writer.write(chars);
        writer.close();

        // Setup the mock
        doReturn("test-bogus-file").when(fileMock).getName();
        doReturn(Paths.get("./test-bogus-file")).when(fileMock).toPath();

        final byte[] hash = FileDownloadSession.calculateMD5HashForBytes(new byte[]{100, 100, 100, 100, 100});

        // Setup the mock calls
        when(managementMock.getFile(anyString()))
                .thenReturn(fileMock);

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash(Base64.encodeBytes(hash));
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Call the method
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);
        verify(clientMock, times(1))
                .publish(eq(FileManagementProtocol.FILE_UPLOAD_STATUS + clientMock.getClientId()), any());

        // Delete the bogus file
        File file = new File("./test-bogus-file");
        file.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void initializationMessageFileExistsAndHashesDontMatch() throws IOException, MqttException {
        // Create the bogus file
        char[] chars = new char[]{100, 100, 100, 100, 100};
        FileWriter writer = new FileWriter("test-bogus-file");
        writer.write(chars);
        writer.close();

        // Setup the mock
        doReturn("test-bogus-file").when(fileMock).getName();
        doReturn(Paths.get("./test-bogus-file")).when(fileMock).toPath();

        // Setup the mock calls
        when(managementMock.getFile(anyString()))
                .thenReturn(fileMock);

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash(Base64.encode("not-matching-hash"));
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Call the method
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);
        verify(clientMock, times(1))
                .publish(eq(FileManagementProtocol.FILE_UPLOAD_STATUS + clientMock.getClientId()), any());

        // Delete the bogus file
        File file = new File("./test-bogus-file");
        file.delete();
    }

    @Test
    public void initializationMessageHandlingHappyFlow() throws MqttException, InterruptedException {
        // Setup the mock calls
        when(managementMock.getFile(anyString()))
                .thenReturn(null);

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Call the method
        protocol.handleFileTransferInitiation(FileManagementProtocol.FILE_UPLOAD_INITIATE + "test", testMessage);

        // Sleep just a tad bit
        Thread.sleep(1000);

        // Verify all the mock calls
        verify(managementMock, times(1)).getFile(anyString());
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void abortMessageWhenSessionIsNull() throws NoSuchFieldException, IllegalAccessException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the field to null
        Field field = FileManagementProtocol.class.getDeclaredField("fileDownloadSession");
        field.setAccessible(true);
        field.set(protocol, null);

        // Create the abort message
        FileAbort testAbortMessage = new FileAbort();
        testAbortMessage.setFileName("test-file");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testAbortMessage));

        // Call everything
        protocol.handleFileTransferAbort(
                FileManagementProtocol.FILE_UPLOAD_ABORT + clientMock.getClientId(), testMessage);
    }

    @Test
    public void abortMessageFileNamesDontMatch() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Do the call
        protocol.handleFileTransferInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_INITIATE, testMessage);

        testMessage = new MqttMessage(JsonUtil.serialize("non-matching-name"));

        // Do the abort
        protocol.handleFileTransferAbort(
                FileManagementProtocol.FILE_UPLOAD_ABORT + clientMock.getClientId(), testMessage);

        // Sleep just a tad bit
        Thread.sleep(1000);

        // Verify the mock calls
        verify(managementMock, times(1)).getFile(anyString());
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void abortMessageHappyFlow() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Do the call
        protocol.handleFileTransferInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_INITIATE, testMessage);

        testMessage = new MqttMessage(JsonUtil.serialize("test-file-message"));

        // Do the abort
        protocol.handleFileTransferAbort(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_ABORT, testMessage);

        // Sleep just a tad bit
        Thread.sleep(1000);

        // Verify the mock calls
        verify(managementMock, times(1)).getFile(anyString());
        verify(clientMock, times(5)).getClientId();
        verify(clientMock, times(3)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFinishNullCheckSession() {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Null check
        exceptionRule.expect(IllegalStateException.class);
        protocol.handleFileTransferFinish(null, FileTransferStatus.FILE_READY, null);
    }

    @Test
    public void handleFinishNullCheckStatus() {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");

        // Null check
        exceptionRule.expect(IllegalStateException.class);
        protocol.handleFileTransferFinish(new FileDownloadSession(testInitMessage,
                new FileDownloadSession.Callback() {
                    @Override
                    public void sendRequest(String fileName, int chunkIndex) {

                    }

                    @Override
                    public void onFinish(FileTransferStatus status, FileTransferError error) {

                    }
                }, protocol.maxChunkSize), null, null);
    }

    @Test
    public void binaryResponseNoSession() throws MqttException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the handler
        protocol.handleFileTransferBinaryResponse(FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_ABORT,
                new MqttMessage(new byte[1001]));

        // Verify no publishes have been called
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void binaryResponseSession() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("test-file-message");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash("abcde");
        MqttMessage testMessage = new MqttMessage(JsonUtil.serialize(testInitMessage));

        // Do the call
        protocol.handleFileTransferInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_INITIATE, testMessage);

        // Call the handler
        protocol.handleFileTransferBinaryResponse(FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_ABORT,
                new MqttMessage(new byte[1088]));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify no publishes have been called
        verify(clientMock, times(5)).getClientId();
        verify(clientMock, times(3)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void fileTransferSessionHappyFlow() throws MqttException, IOException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Create the payload
        byte[] bytes = new byte[1088];
        byte[] actualBytes = new byte[1024];
        byte[] hash = FileDownloadSession.calculateMD5HashForBytes(actualBytes);
        byte[] byteHash = DigestUtils.sha256(actualBytes);
        for (int i = 0; i < byteHash.length; i++) {
            bytes[bytes.length - byteHash.length + i] = byteHash[i];
        }

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("Test File");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash(DatatypeConverter.printHexBinary(hash));

        // Do the calls
        protocol.handleFileTransferInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize(testInitMessage)));

        protocol.handleFileTransferBinaryResponse(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_BINARY_RESPONSE,
                new MqttMessage(bytes));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the calls
        verify(managementMock, times(1)).createFile(any(), anyString());
        verify(clientMock, times(6)).getClientId();
        verify(clientMock, times(4)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void fileTransferSessionFailToSaveFile() throws MqttException, IOException, InterruptedException {
        // Create the snap
        doThrow(new IOException("Failed to save file - TEST.")).when(managementMock).createFile(any(), anyString());

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Create the payload
        byte[] bytes = new byte[1088];
        byte[] actualBytes = new byte[1024];
        byte[] hash = FileDownloadSession.calculateMD5HashForBytes(actualBytes);
        byte[] byteHash = DigestUtils.sha256(actualBytes);
        for (int i = 0; i < byteHash.length; i++) {
            bytes[bytes.length - byteHash.length + i] = byteHash[i];
        }

        // Prepare the message
        FileInit testInitMessage = new FileInit();
        testInitMessage.setFileName("Test File");
        testInitMessage.setFileSize(1024);
        testInitMessage.setFileHash(DatatypeConverter.printHexBinary(hash));

        // Do the calls
        protocol.handleFileTransferInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_UPLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize(testInitMessage)));

        protocol.handleFileTransferBinaryResponse(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_ABORT,
                new MqttMessage(bytes));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the calls
        verify(clientMock, times(6)).getClientId();
        verify(clientMock, times(4)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlInitializationSessionRunning() throws NoSuchFieldException, IllegalAccessException, MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the session
        Field field = FileManagementProtocol.class.getDeclaredField("urlFileDownloadSession");
        field.setAccessible(true);
        field.set(protocol, urlFileDownloadSessionMock);

        // Prepare the test message
        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setFileUrl("https://test.url");

        // Call the method
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize(urlInfo)));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify the mocks were called
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlInitializationManagementIsNull() throws NoSuchFieldException, IllegalAccessException, MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the session
        Field field = FileManagementProtocol.class.getDeclaredField("management");
        field.setAccessible(true);
        field.set(protocol, null);

        // Call the method
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify everything was called on the mocks
        verify(clientMock, times(2)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlInitializationManagementPublishThrows() throws MqttException, InterruptedException {
        // Setup the throw
        doThrow(new MqttException(new Exception("Test MQTT Exception."))).when(clientMock)
                .publish(anyString(), any(), anyInt(), anyBoolean());

        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify everything was called on the mocks
        verify(clientMock, times(3)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlInitializationManagementHappyFlow() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the mock calls
        verify(clientMock, times(3)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlAbortButNoSession() throws NoSuchFieldException, MqttException, IllegalAccessException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the session
        Field field = FileManagementProtocol.class.getDeclaredField("urlFileDownloadSession");
        field.setAccessible(true);
        field.set(protocol, null);

        // Call the method
        protocol.handleUrlDownloadAbort(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify the mocks were called properly
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlAbortWrongUrl() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the init
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://proper.test.url")));

        // Call the method
        protocol.handleUrlDownloadAbort(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://notproper.test.url")));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify the mocks were called properly
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void urlAbortHappyFlow() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the init
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Call the method
        protocol.handleUrlDownloadAbort(FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_ABORT,
                new MqttMessage(JsonUtil.serialize("https://test.url")));

        // Sleep for a tad bit
        Thread.sleep(1000);

        // Verify the mocks were called appropriately
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(2)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFileDeleteNoManagement() throws MqttException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the management to null
        Field field = FileManagementProtocol.class.getDeclaredField("management");
        field.setAccessible(true);
        field.set(protocol, null);

        // Create the test message
        FileDelete delete = new FileDelete();
        delete.setFileNames(Collections.singletonList("test-file"));

        // Call the method
        protocol.handleFileDeletion(FileManagementProtocol.FILE_DELETE + clientMock.getClientId(),
                new MqttMessage(JsonUtil.serialize(delete)));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the mocks have been called
        verify(managementMock, never()).deleteFile(eq("test-file"));
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void nullCheckUrlFinishSession() {
        // Setup the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the finish
        exceptionRule.expect(IllegalStateException.class);
        protocol.handleUrlSessionFinish(null, FileTransferStatus.FILE_READY, "", null);
    }

    @Test
    public void nullCheckUrlFinishStats() {
        // Setup the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the finish
        exceptionRule.expect(IllegalStateException.class);
        protocol.handleUrlSessionFinish(urlFileDownloadSessionMock, null, "", null);
    }

    @Test
    public void handleUrlFileHappyFlow() throws InterruptedException, IOException, MqttException {
        // Setup the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Pass the initialize message
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://get.docker.com")));

        // Wait for the results to show up
        Thread.sleep(1000);

        // Check that everything got called
        verify(managementMock, times(1)).createFile(any(), eq("get.docker.com"));
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(3)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleUrlFileThrowsSaveFile() throws InterruptedException, IOException, MqttException {
        // Setup the throw
        doThrow(new IOException("Test FILE Exception")).when(managementMock).createFile(any(), anyString());

        // Setup the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Pass the initialize message
        protocol.handleUrlDownloadInitiation(
                FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_URL_DOWNLOAD_INITIATE,
                new MqttMessage(JsonUtil.serialize("https://get.docker.com")));

        // Wait for the results to show up
        Thread.sleep(1000);

        // Check that everything got called
        verify(clientMock, times(4)).getClientId();
        verify(clientMock, times(3)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFileDelete() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.handleFileDeletion(FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_DELETE,
                new MqttMessage(JsonUtil.serialize(Collections.singletonList("test-file"))));

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the mocks have been called
        verify(managementMock, times(1)).deleteFile(eq("test-file"));
        verify(clientMock, times(2)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFilePurgeNoManagement() throws MqttException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Set the management to null
        Field field = FileManagementProtocol.class.getDeclaredField("management");
        field.setAccessible(true);
        field.set(protocol, null);

        // Call the method
        protocol.handleFilePurge(FileManagementProtocol.FILE_PURGE + clientMock.getClientId(),
                new MqttMessage());

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the mocks have been called
        verify(managementMock, never()).purgeDirectory();
        verify(clientMock, times(1)).getClientId();
        verify(clientMock, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFilePurge() throws MqttException, InterruptedException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.handleFilePurge(FileManagementProtocol.FILE_PURGE + clientMock.getClientId(),
                new MqttMessage());

        // Sleep a tad bit
        Thread.sleep(1000);

        // Verify the mocks have been called
        verify(managementMock, times(1)).purgeDirectory();
        verify(clientMock, times(2)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    public void handleFileRequest() throws IOException, MqttException {
        // Create the protocol
        protocol = new FileManagementProtocol(clientMock, managementMock);

        // Call the method
        protocol.handleFileListRequest(FileManagementProtocol.IN_DIRECTION + clientMock.getClientId() + FileManagementProtocol.FILE_LIST,
                new MqttMessage());

        // Verify the mocks have been called
        verify(managementMock, times(1)).listAllFiles();
        verify(clientMock, times(2)).getClientId();
        verify(clientMock, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }
}
