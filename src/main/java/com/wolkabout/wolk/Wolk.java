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

import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.FileSystemManagement;
import com.wolkabout.wolk.filemanagement.UrlFileDownloader;
import com.wolkabout.wolk.firmwareupdate.FirmwareInstaller;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateProtocol;
import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.persistence.InMemoryPersistence;
import com.wolkabout.wolk.persistence.Persistence;
import com.wolkabout.wolk.protocol.Protocol;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.WolkaboutProtocol;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles the connection to the WolkAbout IoT Platform.
 */
public class Wolk {

    public static final String WOLK_DEMO_URL = "ssl://api-demo.wolkabout.com:8883";
    public static final String WOLK_DEMO_CA = "ca.crt";
    private static final Logger LOG = LoggerFactory.getLogger(Wolk.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private OutboundDataMode mode;
    private ScheduledFuture<?> runningPublishTask;
    private ScheduledFuture<?> runningPublishKeepAliveTask;
    /**
     * MQTT client.
     */
    private MqttClient client;
    /**
     * MQTT connect options
     */
    private MqttConnectOptions options;
    /**
     * Protocol for sending and receiving data.
     */
    private Protocol protocol;

    /**
     * Everything regarding the File Management/Firmware Update.
     */
    private boolean fileTransferUrlEnabled;
    private FileManagementProtocol fileManagementProtocol;
    private FileSystemManagement fileSystemManagement;
    private String firmwareVersion;
    private FirmwareUpdateProtocol firmwareUpdateProtocol;
    private FirmwareInstaller firmwareInstaller;
    /**
     * Persistence mechanism for storing and retrieving data.
     */
    private Persistence persistence;
    private final Runnable publishTask = this::publish;

    private boolean firstConnect = true;

    public static Builder builder(OutboundDataMode mode) {
        return new Builder(mode);
    }

    public boolean connect() {
        try {
            client.connect(options);
        } catch (Exception e) {
            LOG.info("Could not connect to MQTT broker.", e);
            return false;
        }

        subscribe();

        if (firstConnect) {
            publishParameters();

            firstConnect = false;
        }

        if (fileManagementProtocol != null) {
            publishFileList();

            if (firmwareUpdateProtocol != null) {
                firmwareUpdateProtocol.checkFirmwareVersion();
                publishFirmwareVersion(firmwareVersion);
            }
        }

        if (mode == OutboundDataMode.PULL) {
            pullParameters();
            pullFeeds();
        }

        return true;
    }

    /**
     * Disconnects from the MQTT broker.
     */
    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            LOG.trace("Could not disconnect from MQTT broker.", e);
        }
    }

    /**
     * Start automatic reading publishing.
     * Readings are published every X seconds.
     * Automatic publishing requires a persistence store.
     *
     * @param seconds Time in seconds between 2 publishes.
     */
    public void startPublishing(int seconds) {
        if (persistence == null) {
            throw new IllegalStateException("Automatic publishing requires persistence store.");
        }

        if (runningPublishTask != null && !runningPublishTask.isDone()) {
            return;
        }

        runningPublishTask = executor.scheduleAtFixedRate(publishTask, 0, seconds, TimeUnit.SECONDS);
    }

    /**
     * Stop automatic reading publishing
     */
    public void stopPublishing() {
        if (runningPublishTask == null || runningPublishTask.isDone()) {
            return;
        }

        runningPublishTask.cancel(true);
    }

    /**
     * Manually publish stored readings.
     * Requires a persistence store.
     */
    public void publish() {
        if (persistence == null) {
            throw new IllegalStateException("Manual publishing requires persistence store.");
        }

        try {
            protocol.publishFeeds(persistence.getAll());
        } catch (Exception e) {
            LOG.info("Could not publish feeds", e);
        }
    }

    /**
     * Adds reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference Reference of the sensor
     * @param value     Value obtained by the reading
     */
    public void addFeed(String reference, boolean value) {
        final Feed feed = new Feed(reference, Boolean.toString(value));
        addFeed(feed);
    }

    public void addFeed(String reference, boolean value, long timestamp) {
        final Feed feed = new Feed(reference, Boolean.toString(value), timestamp);
        addFeed(feed);
    }

    public void addFeed(String reference, long value) {
        final Feed feed = new Feed(reference, Long.toString(value));
        addFeed(feed);
    }

    public void addFeed(String reference, long value, long timestamp) {
        final Feed feed = new Feed(reference, Long.toString(value), timestamp);
        addFeed(feed);
    }

    public void addFeed(String reference, double value) {
        final Feed feed = new Feed(reference, Double.toString(value));
        addFeed(feed);
    }

    public void addFeed(String reference, double value, long timestamp) {
        final Feed feed = new Feed(reference, Double.toString(value), timestamp);
        addFeed(feed);
    }

    public void addFeed(String reference, String value) {
        final Feed feed = new Feed(reference, value);
        addFeed(feed);
    }

    public void addFeed(String reference, String value, long timestamp) {
        final Feed feed = new Feed(reference, value, timestamp);
        addFeed(feed);
    }

    /**
     * Adds multivalue reading to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param reference Reference of the sensor
     * @param values    Values obtained by the reading
     */
    public void addFeed(String reference, List<Object> values) {
        final Feed feed = new Feed(reference, values.stream().map(Object::toString).collect(Collectors.toList()));
        addFeed(feed);
    }

    public void addFeed(String reference, List<Object> values, long timestamp) {
        final Feed feed = new Feed(reference, values.stream().map(Object::toString).collect(Collectors.toList()), timestamp);
        addFeed(feed);
    }

    /**
     * Adds readings to be published.
     * If the persistence store is set, the reading will be stored. Otherwise, it will be published immediately.
     *
     * @param feed {@link Feed}
     */
    public void addFeed(Feed feed) {
        if (persistence != null) {
            persistence.addFeed(feed);
            return;
        }

        try {
            protocol.publishFeed(feed);
        } catch (Exception e) {
            LOG.info("Could not publish reading: " + feed.getReference(), e);
        }
    }

    /**
     * Adds a reading collection to be published.
     * If the persistence store is set, readings will be stored. Otherwise, they will be published immediately.
     *
     * @param feeds A collection of {@link Feed}
     */
    public void addFeeds(Collection<Feed> feeds) {
        if (persistence != null) {
            persistence.addFeeds(feeds);
            return;
        }

        try {
            protocol.publishFeeds(feeds);
        } catch (Exception e) {
            LOG.info("Could not publish feeds", e);
        }
    }

    /**
     * Publishes the current list of files.
     */
    public void publishFileList() {
        if (fileManagementProtocol == null) {
            throw new IllegalStateException("File management protocol is not configured.");
        }

        try {
            fileManagementProtocol.publishFileList();
        } catch (Exception e) {
            LOG.info("Could not publish file list.", e);
        }
    }

    /**
     * Publishes the new version of the firmware.
     *
     * @param version current firmware version.
     */
    public void publishFirmwareVersion(String version) {
        if (fileManagementProtocol == null) {
            this.firmwareVersion = version;
            throw new IllegalStateException("Firmware update protocol is not configured.");
        }

        try {
            firmwareUpdateProtocol.publishFirmwareVersion(version);
        } catch (Exception e) {
            LOG.info("Could not publish firmware version", e);
        }
    }

    public void pullFeeds() {
        protocol.pullFeeds();
    }

    public void pullParameters() {
        protocol.pullParameters();
    }

    public void pullTime() {
        protocol.pullTime();
    }

    /**
     * Register new attribute or update an existing one
     * @param attribute
     */
    public void registerAttribute(Attribute attribute) {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(attribute);

        protocol.registerAttributes(attributes);
    }

    /**
     * Register new attribute or update an existing one
     * @param name
     * @param type
     * @param value
     */
    public void registerAttribute(String name, DataType type, String value) {
        registerAttribute(new Attribute(name, type, value));
    }

    private void subscribe() {
        try {
            protocol.subscribe();

            if (fileManagementProtocol != null) {
                fileManagementProtocol.subscribe();
            }

            if (firmwareUpdateProtocol != null) {
                firmwareUpdateProtocol.subscribe();
            }
        } catch (Exception e) {
            LOG.debug("Unable to subscribe to all required topics.", e);
        }
    }

    private void publishParameters() {
        List<Parameter> parameters = new ArrayList<>();

        parameters.add(new Parameter(Parameter.Name.OUTBOUND_DATA_MODE.name(), mode));

        final boolean fileEnabled = fileSystemManagement != null;
        parameters.add(new Parameter(Parameter.Name.FILE_TRANSFER_PLATFORM_ENABLED.name(), fileEnabled));
        parameters.add(new Parameter(Parameter.Name.FILE_TRANSFER_URL_ENABLED.name(), fileEnabled && fileTransferUrlEnabled));

        final boolean firmwareEnabled = firmwareInstaller != null;
        parameters.add(new Parameter(Parameter.Name.FIRMWARE_UPDATE_ENABLED.name(), firmwareEnabled));

        if (firmwareEnabled) {
            parameters.add(new Parameter(Parameter.Name.FIRMWARE_VERSION.name(), firmwareVersion));
        }

        protocol.updateParameters(parameters);
    }

    public static class Builder {

        private static final String DEFAULT_FILE_LOCATION = "files/";
        private final MqttBuilder mqttBuilder = new MqttBuilder(this);
        private OutboundDataMode mode;
        private ProtocolType protocolType = ProtocolType.WOLKABOUT_PROTOCOL;
        private Collection<String> actuatorReferences = new ArrayList<>();

        private FeedHandler feedHandler = new FeedHandler() {
            @Override
            public void onFeedsReceived(Collection<Feed> feeds) {
                LOG.trace("Feeds received: " + feeds);
            }

            @Override
            public Feed getFeedValue(String reference) { return null; }
        };

        private Persistence persistence = new InMemoryPersistence();

        private boolean fileManagementEnabled = false;

        private String fileManagementLocation = null;

        private boolean firmwareUpdateEnabled = false;

        private String firmwareVersion = "";

        private UrlFileDownloader urlFileDownloader = null;

        private FirmwareInstaller firmwareInstaller = null;

        private Builder(OutboundDataMode mode) {
            this.mode = mode;
        }

        public MqttBuilder mqtt() {
            return mqttBuilder;
        }

        public Builder protocol(ProtocolType protocolType) {
            if (protocolType == null) {
                throw new IllegalArgumentException("Protocol type cannot be null.");
            }

            this.protocolType = protocolType;
            return this;
        }

//        public Builder actuator(Collection<String> actuatorReferences, ActuatorHandler actuatorHandler) {
//            if (actuatorReferences.isEmpty()) {
//                throw new IllegalArgumentException("Actuator references must be set.");
//            }
//
//            if (actuatorHandler == null) {
//                throw new IllegalArgumentException("Actuator handler must be set.");
//            }
//
//            this.actuatorReferences = actuatorReferences;
//            this.actuatorHandler = actuatorHandler;
//            return this;
//        }

        public Builder feed(FeedHandler feedHandler) {
            if (feedHandler == null) {
                throw new IllegalArgumentException("Feed handler must be set.");
            }

            this.feedHandler = feedHandler;
            return this;
        }

        public Builder persistence(Persistence persistence) {
            if (persistence == null) {
                throw new IllegalArgumentException("Persistence must be set.");
            }

            this.persistence = persistence;
            return this;
        }

        public Builder enableFileManagement() {
            fileManagementEnabled = true;
            return this;
        }

        public Builder enableFileManagement(String fileManagementLocation) {
            fileManagementEnabled = true;
            this.fileManagementLocation = fileManagementLocation;
            return this;
        }

        public Builder enableFileManagement(UrlFileDownloader urlFileDownloader) {
            fileManagementEnabled = true;
            this.urlFileDownloader = urlFileDownloader;
            return this;
        }

        public Builder enableFileManagement(String fileManagementLocation, UrlFileDownloader urlFileDownloader) {
            fileManagementEnabled = true;
            this.fileManagementLocation = fileManagementLocation;
            this.urlFileDownloader = urlFileDownloader;
            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, String firmwareVersion) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            fileManagementEnabled = true;
            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder enableFirmwareUpdate(String fileManagementLocation, String firmwareVersion,
                                            FirmwareInstaller firmwareInstaller) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            fileManagementEnabled = true;
            this.fileManagementLocation = fileManagementLocation;
            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder enableFirmwareUpdate(UrlFileDownloader urlFileDownloader, String firmwareVersion,
                                            FirmwareInstaller firmwareInstaller) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            fileManagementEnabled = true;
            this.urlFileDownloader = urlFileDownloader;
            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder enableFirmwareUpdate(String fileManagementLocation, UrlFileDownloader urlFileDownloader,
                                            String firmwareVersion, FirmwareInstaller firmwareInstaller) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            fileManagementEnabled = true;
            this.fileManagementLocation = fileManagementLocation;
            this.urlFileDownloader = urlFileDownloader;
            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Wolk build() {

            try {
                final Wolk wolk = new Wolk();
                wolk.mode = mode;
                wolk.client = mqttBuilder.client();
                wolk.client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (reconnect) {
                            wolk.subscribe();
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });

                wolk.options = mqttBuilder.options();
                wolk.protocol = getProtocol(wolk.client);
                wolk.persistence = persistence;

                if (fileManagementEnabled) {
                    // Create the file system management
                    wolk.fileSystemManagement = new FileSystemManagement(
                            fileManagementLocation.isEmpty() ? DEFAULT_FILE_LOCATION : fileManagementLocation);

                    // Create the file management protocol
                    if (this.urlFileDownloader == null) {
                        wolk.fileManagementProtocol =
                                new FileManagementProtocol(wolk.client, wolk.fileSystemManagement);
                        wolk.fileTransferUrlEnabled = false;
                    } else {
                        wolk.fileManagementProtocol =
                                new FileManagementProtocol(wolk.client, wolk.fileSystemManagement, urlFileDownloader);
                        wolk.fileTransferUrlEnabled = true;
                    }

                    // Create the firmware update if that is something the user wants
                    if (firmwareUpdateEnabled) {
                        wolk.firmwareInstaller = firmwareInstaller;
                        wolk.firmwareVersion = firmwareVersion;
                        wolk.firmwareUpdateProtocol = new FirmwareUpdateProtocol(
                                wolk.client, wolk.fileSystemManagement, wolk.firmwareInstaller);
                    }
                }

                feedHandler.setWolk(wolk);

                return wolk;
            } catch (MqttException mqttException) {
                throw new IllegalArgumentException("Unable to create MQTT connection.", mqttException);
            }
        }

        private Protocol getProtocol(MqttClient client) {
            if (protocolType == ProtocolType.WOLKABOUT_PROTOCOL) {
                return new WolkaboutProtocol(client, feedHandler);
            }
            throw new IllegalArgumentException("Unknown protocol type: " + protocolType);

        }
    }
}
