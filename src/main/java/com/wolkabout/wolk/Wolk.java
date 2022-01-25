/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.wolkabout.wolk.filemanagement.FileManagementProtocol;
import com.wolkabout.wolk.filemanagement.FileSystemManagement;
import com.wolkabout.wolk.filemanagement.UrlFileDownloader;
import com.wolkabout.wolk.firmwareupdate.FirmwareInstaller;
import com.wolkabout.wolk.firmwareupdate.FirmwareManagement;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateProtocol;
import com.wolkabout.wolk.firmwareupdate.ScheduledFirmwareUpdate;
import com.wolkabout.wolk.model.*;
import com.wolkabout.wolk.persistence.InMemoryPersistence;
import com.wolkabout.wolk.persistence.Persistence;
import com.wolkabout.wolk.protocol.Protocol;
import com.wolkabout.wolk.protocol.ProtocolType;
import com.wolkabout.wolk.protocol.WolkaboutProtocol;
import com.wolkabout.wolk.protocol.handler.ErrorHandler;
import com.wolkabout.wolk.protocol.handler.FeedHandler;
import com.wolkabout.wolk.protocol.handler.ParameterHandler;
import com.wolkabout.wolk.protocol.handler.TimeHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.field.expression.FieldExpressionFactory.*;

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
    private Cron firmwareUpdateTime;
    private String firmwareUpdateRepository;
    private FirmwareUpdateProtocol firmwareUpdateProtocol;
    private FirmwareInstaller firmwareInstaller;
    private FirmwareManagement firmwareManagement;
    private ScheduledFirmwareUpdate scheduledFirmwareUpdate;
    private final CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    /**
     * Persistence mechanism for storing and retrieving data.
     */
    private Persistence persistence;
    private int maxMessageSize;
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

            if (fileManagementProtocol != null) {
                publishFileList();

                if (firmwareUpdateProtocol != null) {
                    firmwareUpdateProtocol.checkFirmwareVersion();
                }
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
        final Feed feed = new Feed(reference, value);
        addFeed(feed);
    }

    public void addFeed(String reference, boolean value, long timestamp) {
        final Feed feed = new Feed(reference, value, timestamp);
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
        final Feed feed = new Feed(reference, value);
        addFeed(feed);
    }

    public void addFeed(String reference, double value, long timestamp) {
        final Feed feed = new Feed(reference, value, timestamp);
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
        final Feed feed = new Feed(reference, values);
        addFeed(feed);
    }

    public void addFeed(String reference, List<Object> values, long timestamp) {
        final Feed feed = new Feed(reference, values, timestamp);
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

    public void pullFeeds() {
        protocol.pullFeeds();
    }

    public void pullParameters() {
        protocol.pullParameters();
    }

    public void syncParameters(Collection<String> parameterNames) {
        protocol.syncronizeParameters(parameterNames);
    }

    public void pullTime() {
        protocol.pullTime();
    }

    public void registerFeed(String name, FeedType type, String unit, String reference) {
        registerFeed(new FeedTemplate(name, type, unit, reference));
    }

    public void registerFeed(String name, FeedType type, Unit unit, String reference) {
        registerFeed(new FeedTemplate(name, type, unit, reference));
    }

    public void registerFeed(FeedTemplate feed) {
        registerFeeds(Collections.singletonList(feed));
    }

    public void registerFeeds(Collection<FeedTemplate> feeds) {
        protocol.registerFeeds(feeds);
    }

    public void removeFeed(String feedReference) {
        removeFeeds(Collections.singletonList(feedReference));
    }

    public void removeFeeds(Collection<String> feedReferences) {
        protocol.removeFeeds(feedReferences);
    }

    /**
     * Register new attribute or update an existing one
     *
     * @param name
     * @param type
     * @param value
     */
    public void registerAttribute(String name, DataType type, String value) {
        registerAttribute(new Attribute(name, type, value));
    }

    /**
     * Register new attribute or update an existing one
     *
     * @param attribute
     */
    public void registerAttribute(Attribute attribute) {
        protocol.registerAttributes(Collections.singletonList(attribute));
    }

    /**
     * Register new attributes or update the existing ones
     *
     * @param attributes
     */
    public void registerAttributes(Collection<Attribute> attributes) {
        protocol.registerAttributes(attributes);
    }

    public void checkAndUpdateFirmware() {
        checkAndUpdateFirmware(firmwareUpdateRepository);
    }

    public void checkAndUpdateFirmware(String repository) {
        if (firmwareManagement == null) {
            throw new IllegalStateException("Firmware update is not configured.");
        }

        firmwareManagement.checkAndInstall(repository);
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
            parameters.add(new Parameter(Parameter.Name.FIRMWARE_VERSION.name(), firmwareInstaller.getFirmwareVersion()));
            parameters.add(new Parameter(Parameter.Name.FIRMWARE_UPDATE_CHECK_TIME.name(), firmwareUpdateTime != null ? firmwareUpdateTime.asString() : ""));
            parameters.add(new Parameter(Parameter.Name.FIRMWARE_UPDATE_REPOSITORY.name(), firmwareUpdateRepository));
        }

        parameters.add(new Parameter(Parameter.Name.MAXIMUM_MESSAGE_SIZE.name(), maxMessageSize));

        protocol.updateParameters(parameters);
    }

    private void onParameters(Collection<Parameter> parameters) {
        LOG.debug("Parameters received " + parameters);

        parameters.forEach(parameter -> {
            if (parameter.getReference().equals(Parameter.Name.FIRMWARE_UPDATE_CHECK_TIME.name())) {
                onFirmwareUpdateCheckTime(parameter.getValue());
            } else if (parameter.getReference().equals(Parameter.Name.FIRMWARE_UPDATE_REPOSITORY.name())) {
                onFirmwareUpdateRepository((String) parameter.getValue());
            } else {
                LOG.warn("Unable to handle parameter change: " + parameter);
            }
        });
    }

    void onFirmwareUpdateCheckTime(Object time) {
        LOG.debug("Setting firmware update check time");

        if (scheduledFirmwareUpdate == null) {
            LOG.debug("Skip setting firmware check time, scheduled firmware update not enabled");
            return;
        }

        try {
            final String cronStr = (String) time;

            Cron cron = cronParser.parse(cronStr);

            if (firmwareUpdateTime != null && firmwareUpdateTime.equivalent(cron)) {
                LOG.debug("Firmware check time already has same value: " + time);
                return;
            }

            firmwareUpdateTime = cron;

            scheduledFirmwareUpdate.setTimeAndReschedule(firmwareUpdateTime);
        } catch (Exception exception) {
            LOG.error("Firmware check time has invalid cron value: " + time);

            firmwareUpdateTime = null;
            scheduledFirmwareUpdate.setTimeAndReschedule(null);
        }
    }

    void onFirmwareUpdateRepository(String repository) {
        if (scheduledFirmwareUpdate == null) {
            LOG.debug("Skip setting firmware update repository, scheduled firmware update not enabled");
            return;
        }

        scheduledFirmwareUpdate.setRepository(repository);
    }

    public static class Builder {

        private static final String DEFAULT_FILE_LOCATION = "files/";
        private final MqttBuilder mqttBuilder = new MqttBuilder(this);
        private final OutboundDataMode mode;
        private ProtocolType protocolType = ProtocolType.WOLKABOUT_PROTOCOL;

        private int maxMessageSize = 0;

        private FeedHandler feedHandler = new FeedHandler() {
            @Override
            public void onFeedsReceived(Collection<Feed> feeds) {
                LOG.debug("Feeds received: " + feeds);
            }

            @Override
            public Feed getFeedValue(String reference) {
                return null;
            }
        };

        private ParameterHandler parameterHandler;

        private TimeHandler timeHandler = new TimeHandler() {
            @Override
            public void onTimeReceived(long timestamp) {
                LOG.debug("Time received: " + timestamp);
            }
        };

        private ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public void onErrorReceived(String error) {
                LOG.debug("Error received: " + error);
            }
        };

        private Persistence persistence = new InMemoryPersistence();

        private boolean fileManagementEnabled = false;
        private boolean defaultUrlFileDownloaderEnabled = true;
        private String fileManagementLocation = "";
        private UrlFileDownloader urlFileDownloader = null;

        private boolean firmwareUpdateEnabled = false;
        private FirmwareInstaller firmwareInstaller = null;
        private Cron firmwareUpdateTime = null;
        private String firmwareUpdateRepository = "";

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

        public Builder feed(FeedHandler feedHandler) {
            if (feedHandler == null) {
                throw new IllegalArgumentException("Feed handler must be set.");
            }

            this.feedHandler = feedHandler;
            return this;
        }

        public Builder parameters(ParameterHandler parameterHandler) {
            if (parameterHandler == null) {
                throw new IllegalArgumentException("Parameter handler must be set.");
            }

            this.parameterHandler = parameterHandler;
            return this;
        }

        public Builder time(TimeHandler timeHandler) {
            if (timeHandler == null) {
                throw new IllegalArgumentException("Time handler must be set.");
            }

            this.timeHandler = timeHandler;
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

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, String firmwareUpdateRepository,
                                            LocalTime firmwareUpdateDailyTime) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareUpdateRepository = firmwareUpdateRepository;

            this.firmwareUpdateTime = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                    .withYear(always())
                    .withDoM(always())
                    .withMonth(always())
                    .withDoW(questionMark())
                    .withHour(on(firmwareUpdateDailyTime.getHour()))
                    .withMinute(on(firmwareUpdateDailyTime.getMinute()))
                    .withSecond(on(firmwareUpdateDailyTime.getSecond()))
                    .instance();

            return this;
        }

        public Builder enableFirmwareUpdate(FirmwareInstaller firmwareInstaller, String firmwareUpdateRepository,
                                            String firmwareUpdateCron) {
            if (firmwareInstaller == null) {
                throw new IllegalArgumentException("FirmwareInstaller is required to enable firmware updates.");
            }

            firmwareUpdateEnabled = true;
            this.firmwareInstaller = firmwareInstaller;
            this.firmwareUpdateRepository = firmwareUpdateRepository;

            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
            this.firmwareUpdateTime = parser.parse(firmwareUpdateCron);

            return this;
        }

        /**
         * Maximum size of message that can be received in kilobytes
         * This also applies to maximum chunk size of file
         *
         * @param maxMessageSize
         * @return
         */
        public Builder maxMessageKiloBytes(int maxMessageSize) {
            if (maxMessageSize < 0) {
                throw new IllegalArgumentException("Max message size must be a non negative number");
            }

            this.maxMessageSize = maxMessageSize;
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

                boolean internalParameterHandler = false;

                if (parameterHandler == null) {
                    parameterHandler = wolk::onParameters;
                    internalParameterHandler = true;
                }

                wolk.protocol = getProtocol(wolk.client);
                wolk.persistence = persistence;
                wolk.maxMessageSize = maxMessageSize;

                setupFileManagement(wolk);
                setupFirmwareUpdate(wolk);

                if (internalParameterHandler) {
                    setupScheduledFirmwareUpdate(wolk);
                }

                return wolk;
            } catch (MqttException mqttException) {
                throw new IllegalArgumentException("Unable to create MQTT connection.", mqttException);
            }
        }

        void setupFileManagement(Wolk wolk) {
            if (!fileManagementEnabled) {
                LOG.debug("File management not enabled");
                return;
            }

            // Create the file system management
            wolk.fileSystemManagement = new FileSystemManagement(
                    fileManagementLocation.isEmpty() ? DEFAULT_FILE_LOCATION : fileManagementLocation);

            // Create the file management protocol
            if (this.urlFileDownloader == null) {
                LOG.debug("Using default url downloader");
                wolk.fileTransferUrlEnabled = defaultUrlFileDownloaderEnabled;
                wolk.fileManagementProtocol = new FileManagementProtocol(wolk.client, wolk.fileSystemManagement);
            } else {
                wolk.fileManagementProtocol = new FileManagementProtocol(wolk.client, wolk.fileSystemManagement, urlFileDownloader);
                wolk.fileTransferUrlEnabled = true;
            }

            wolk.fileManagementProtocol.setMaxChunkSize(maxMessageSize);
        }

        void setupFirmwareUpdate(Wolk wolk) {
            if (!firmwareUpdateEnabled) {
                LOG.debug("Firmware update not enabled");
                return;
            }

            if (!fileManagementEnabled) {
                throw new IllegalArgumentException("FileManagement is required to enable firmware update");
            }

            wolk.firmwareInstaller = firmwareInstaller;
            wolk.firmwareUpdateProtocol = new FirmwareUpdateProtocol(wolk.client, wolk.fileSystemManagement, wolk.firmwareInstaller);
            wolk.firmwareManagement = new FirmwareManagement(wolk.firmwareInstaller, wolk.firmwareUpdateProtocol, wolk.fileManagementProtocol);
        }

        void setupScheduledFirmwareUpdate(Wolk wolk) {
            if (!firmwareUpdateEnabled || !wolk.fileTransferUrlEnabled) {
                LOG.debug("Scheduled firmware update not enabled");
                return;
            }

            if (!fileManagementEnabled) {
                throw new IllegalArgumentException("FileManagement is required to enable scheduled firmware update");
            }

            wolk.firmwareUpdateTime = firmwareUpdateTime;
            wolk.firmwareUpdateRepository = firmwareUpdateRepository;
            wolk.scheduledFirmwareUpdate = new ScheduledFirmwareUpdate(wolk.firmwareManagement, Executors.newScheduledThreadPool(1), wolk.firmwareUpdateRepository, wolk.firmwareUpdateTime);
        }

        private Protocol getProtocol(MqttClient client) {
            if (protocolType == ProtocolType.WOLKABOUT_PROTOCOL) {
                return new WolkaboutProtocol(client, feedHandler, timeHandler, parameterHandler, errorHandler);
            }
            throw new IllegalArgumentException("Unknown protocol type: " + protocolType);

        }
    }
}
