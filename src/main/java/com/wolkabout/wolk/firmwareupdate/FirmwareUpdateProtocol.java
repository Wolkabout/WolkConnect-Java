package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.firmwareupdate.model.FirmwareStatus;
import com.wolkabout.wolk.firmwareupdate.model.StatusResponse;
import com.wolkabout.wolk.firmwareupdate.model.UpdateError;
import com.wolkabout.wolk.firmwareupdate.model.command.Command;
import com.wolkabout.wolk.firmwareupdate.model.command.FileInfo;
import com.wolkabout.wolk.firmwareupdate.model.command.UrlInfo;
import com.wolkabout.wolk.util.JsonUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Arrays;

public class FirmwareUpdateProtocol {

    private static final String FILE_INFO_COMMAND = "FILE_UPLOAD";
    private static final String URL_INFO_COMMAND = "URL_DOWNLOAD";
    private static final String INSTALL_COMMAND = "INSTALL";
    private static final String ABORT_COMMAND = "ABORT";

    public static final CommandReceivedProcessor LOGGING_PROCESSOR = new CommandReceivedProcessor() {
        @Override
        public void onFileReady(byte[] bytes) {
            LOG.trace("File received: " + Arrays.toString(bytes));
        }

        @Override
        public void onInstallCommandReceived() {
            LOG.trace("Install command received.");
        }

        @Override
        public void onAbortCommandReceived() {
            LOG.trace("Abort command received.");
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(FirmwareUpdateProtocol.class);

    private final MqttClient client;
    private final FileDownloader fileDownloader;
    private final CommandReceivedProcessor commandReceivedProcessor;

    public FirmwareUpdateProtocol(MqttClient client, final CommandReceivedProcessor commandReceivedProcessor) {
        this.client = client;
        this.commandReceivedProcessor = commandReceivedProcessor;

        this.fileDownloader = new FileDownloader(client, new FileDownloader.Callback() {
            @Override
            public void onStatusUpdate(FirmwareStatus status) {
                final StatusResponse statusResponse = new StatusResponse();
                statusResponse.setStatus(status);
                publishFlowStatus(statusResponse);
            }

            @Override
            public void onError(UpdateError error) {
                final StatusResponse errorStatus = new StatusResponse();
                errorStatus.setStatus(FirmwareStatus.ERROR);
                errorStatus.setError(error);
                publishFlowStatus(errorStatus);
            }

            @Override
            public void onFileReceived(byte[] bytes) {
                commandReceivedProcessor.onFileReady(bytes);
            }
        });

        subscribe();
    }

    private void subscribe() {
        try {
            client.subscribe("service/commands/firmware/" + client.getClientId(), new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    final Command command = JsonUtil.deserialize(message, Command.class);
                    switch (command.getCommand()) {
                        case FILE_INFO_COMMAND:
                            final FileInfo fileInfo = JsonUtil.deserialize(message, FileInfo.class);
                            fileDownloader.download(fileInfo);
                            break;
                        case URL_INFO_COMMAND:
                            final UrlInfo urlInfo = JsonUtil.deserialize(message, UrlInfo.class);
                            final byte[] bytes = downloadFileFromUrl(urlInfo);
                            commandReceivedProcessor.onFileReady(bytes);
                            break;
                        case INSTALL_COMMAND:
                            commandReceivedProcessor.onInstallCommandReceived();
                            break;
                        case ABORT_COMMAND:
                            commandReceivedProcessor.onAbortCommandReceived();
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown command received: " + command.getCommand());
                    }

                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to subscribe to all required topics.", e);
        }
    }

    public void publishFlowStatus(StatusResponse statusResponse) {
        publish("service/status/firmware/" + client.getClientId(), statusResponse);
    }

    public void publishFirmwareVersion(String version) {
        publish("firmware/version/" + client.getClientId(), version);
    }

    private void publish(String topic, Object payload) {
        try {
            LOG.trace("Publishing to \'" + topic + "\' payload: " + payload);
            client.publish(topic, JsonUtil.serialize(payload), 2, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not publish message to: " + topic + " with payload: " + payload, e);
        }
    }

    private byte[] downloadFileFromUrl(UrlInfo urlInfo) {
        try {
            final URL url = new URL(urlInfo.getFileUrl());
            final InputStream inputStream = url.openStream();
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int read;
            byte[] data = new byte[16384];

            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (Exception e) {
            final StatusResponse errorStatus = new StatusResponse();
            errorStatus.setStatus(FirmwareStatus.ERROR);
            errorStatus.setError(UpdateError.MALFORMED_URL);
            publishFlowStatus(errorStatus);
            return null;
        }
    }
}
