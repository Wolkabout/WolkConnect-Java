package com.wolkabout.wolk;

import java.util.Timer;
import java.util.TimerTask;

public class Wolk {

    private final PublishingService publishingService;
    private final ReadingsBuffer readingsBuffer = new ReadingsBuffer();
    private final Timer timer = new Timer();
    private final TimerTask publishTask = new TimerTask() {
        @Override
        public void run() {
            publish();
        }
    };

    private Logger logger = new Logger() {};

    public Wolk(final Device device) {
        publishingService = new PublishingService(device);
    }

    /**
     * Sets the logging mechanism for the library.
     * @param logger Platform specific implementation of the logging mechanism.
     */
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets a time delta to be used when comparing reading times.
     * If two intervals are within the delta range, the lower time will be set for both readings.
     * Default is 0.
     * @param delta Time interval in seconds.
     */
    public void setTimeDelta(final int delta) {
        readingsBuffer.setDelta(delta);
    }

    /**
     * Starts publishing readings on a given interval.
     * @param interval Time interval in milliseconds to elapse between two publish attempts.
     */
    public void startAutoPublishing(int interval) {
        timer.scheduleAtFixedRate(publishTask, interval, interval);
    }

    /**
     * Cancels a started automatic publishing task.
     */
    public void stopAutoPublishing() {
        publishTask.cancel();
    }

    /**
     * Adds a reading of the given type for the current time.
     * @param type Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final ReadingType type, final String value) {
        readingsBuffer.addReading(type, value);
    }

    /**
     * Adds a reading of the given type for the given time.
     * @param time Time of the reading.
     * @param type Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final long time, final ReadingType type, final String value) {
        readingsBuffer.addReading(time, type, value);
    }

    /**
     * Publishes all the data and clears the reading list if publishing was successful.
     */
    public void publish() {
        if (readingsBuffer.isEmpty()) {
            logger.info("No new readings. Not publishing.");
            return;
        }

        try {
            publishingService.publish(readingsBuffer.getFormattedData());
            readingsBuffer.clear();
            logger.info("Publish successful. Readings list cleared");
        } catch (Exception e) {
            logger.error("Publishing data failed.", e);
        }
    }


}
