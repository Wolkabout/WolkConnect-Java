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

import com.wolkabout.wolk.filemanagement.model.FileTransferStatus;
import com.wolkabout.wolk.filemanagement.model.StatusResponse;
import com.wolkabout.wolk.filemanagement.model.FileTransferError;
import com.wolkabout.wolk.filemanagement.model.command.FileInfo;
import com.wolkabout.wolk.filemanagement.model.command.UrlInfo;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileManagementProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(FileManagementProtocol.class);

    private static final String FILE_UPLOAD_INITIATE = "p2d/file_upload_initiate/d/";
    private static final String FILE_UPLOAD_ABORT = "p2d/file_upload_abort/d/";
    private static final String FILE_URL_DOWNLOAD_INITIATE = "p2d/file_url_download_initiate/d/";
    private static final String FILE_URL_DOWNLOAD_ABORT = "p2d/file_url_download_abort/d/";
    private static final String FILE_UPLOAD_STATUS = "d2p/file_upload_status/d/";
    private static final String FILE_URL_UPLOAD_STATUS = "d2p/file_url_download_status/d/";

    private final MqttClient client;
    private final FileDownloader fileDownloader;
    private final UrlFileDownloader urlDownloader;

    protected static final int QOS = 0;

    public FileManagementProtocol(MqttClient client, UrlFileDownloader urlDownloader) {
        this.client = client;
        this.urlDownloader = urlDownloader;

        this.fileDownloader = new FileDownloader(client, new FileDownloader.Callback() {
            @Override
            public void onStatusUpdate(FileTransferStatus status) {
                final StatusResponse statusResponse = new StatusResponse();
                statusResponse.setStatus(status);
                publishFlowStatus(statusResponse);
            }

            @Override
            public void onError(FileTransferError error) {
                final StatusResponse errorStatus = new StatusResponse();
                errorStatus.setStatus(FileTransferStatus.ERROR);
                errorStatus.setError(error);
                publishFlowStatus(errorStatus);
            }

            @Override
            public void onFileReceived(String fileName, byte[] bytes) {
                publishFlowStatus(FileTransferStatus.FILE_READY, fileName);
                // TODO: callback for reporting file list
            }
        });
    }

    public void subscribe() {
        try {
            client.subscribe(FILE_UPLOAD_INITIATE + client.getClientId(), QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    final FileInfo fileInfo = JsonUtil.deserialize(message, FileInfo.class);
                    fileDownloader.download(fileInfo);
                    // TODO: Move to firmware update
                    //                  firmwareInstaller.onInstallCommandReceived();
                    //                  firmwareInstaller.onAbortCommandReceived();
                }
            });

            client.subscribe(FILE_UPLOAD_ABORT + client.getClientId(), QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    fileDownloader.abort();
                }
            });

            client.subscribe(FILE_URL_DOWNLOAD_INITIATE + client.getClientId(), QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    final UrlInfo urlInfo = JsonUtil.deserialize(message, UrlInfo.class);
                    if (urlDownloader == null) {
                        publishFlowStatus(FileTransferError.FILE_UPLOAD_DISABLED);
                        return;
                    }

                    publishFlowStatus(FileTransferStatus.FILE_TRANSFER, urlInfo.getFileUrl());

                    urlDownloader.downloadFile(urlInfo.getFileUrl(), new UrlFileDownloader.Callback() {
                        @Override
                        public void onError(FileTransferError error) {
                            publishFlowStatus(error);
                        }

                        @Override
                        public void onFileReceived(String fileName, byte[] bytes) {
                            publishFlowStatus(FileTransferStatus.FILE_READY, fileName);
                            // TODO: publish file list
                        }
                    });
                }

                ;
            });

            client.subscribe(FILE_URL_DOWNLOAD_ABORT + client.getClientId(), QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (urlDownloader != null) {
                        final StatusResponse status = new StatusResponse();
                        status.setStatus(FileTransferStatus.ABORTED);
                        if (urlDownloader.getUrl() != null) {
                            status.setFileUrl(urlDownloader.getUrl());
                        }
                        publishFlowStatus(status);
                        urlDownloader.abort();
                    }
                }
            });
            fileDownloader.subscribe();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }

    public void publishFlowStatus(FileTransferStatus status, String fileName) {
        if (status == null) {
            throw new IllegalArgumentException("Status must be set.");
        }

        if (status == FileTransferStatus.ERROR) {
            throw new IllegalArgumentException("Use publishFlowStatus(FileTransferError error) to publish an error state.");
        }

        final StatusResponse statusResponse = new StatusResponse();
        statusResponse.setStatus(status);
        if (fileName.contains("/")) { // File names can't contain slashes, meaning it is a URL
            statusResponse.setFileUrl(fileName);
        }
        else {
            statusResponse.setFileName(fileName);
        }
        publishFlowStatus(statusResponse);
    }

    public void publishFlowStatus(FileTransferError error) {
        if (error == null) {
            throw new IllegalArgumentException("Error must be set.");
        }

        final StatusResponse statusResponse = new StatusResponse();
        statusResponse.setStatus(FileTransferStatus.ERROR);
        statusResponse.setError(error);
        publishFlowStatus(statusResponse);
    }


    private void publishFlowStatus(StatusResponse statusResponse) {
        if (statusResponse.getFileUrl() == null) {
            publish(FILE_UPLOAD_STATUS + client.getClientId(), statusResponse);
        }
        else {
            publish(FILE_URL_UPLOAD_STATUS + client.getClientId(), statusResponse);
        }
    }

    private void publish(String topic, Object payload) {
        try {
            LOG.debug("Publishing to '" + topic + "' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), QOS, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }
}
