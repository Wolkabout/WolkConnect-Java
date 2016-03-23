package com.wolkabout.wolk;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class Wolk {

    private static final String TIME_PREFIX = "R";
    private static final String RTC = "RTC ";
    private static final String MESSAGE_START = "READINGS ";
    private static final String PREFIX_DELIMITER = ":";
    private static final String READING_DELIMITER = ",";
    private static final String TIME_DELIMITER = "|";
    private static final String MESSAGE_END = ";";

    private final NavigableMap<Long, List<Reading>> readings= new TreeMap<>();
    private final PublishingService publishingService;

    private final Timer timer = new Timer();
    private final TimerTask publishTask = new TimerTask() {
        @Override
        public void run() {
            publish();
        }
    };

    private int delta = 0;
    private Logger logger = new Logger() {};

    public Wolk(final Device device) {
        publishingService = new PublishingService(device);
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets a time delta to be used when comparing reading times.
     * If two intervals are within the delta range, the lower time will be set for both readings.
     * @param delta Time interval.
     */
    public void setTimeDelta(final int delta) {
        this.delta = delta;
    }

    /**
     * Starts publishing readings on a given interval.
     * @param interval Time to elapse between two publish attempts.
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
        final long seconds = System.currentTimeMillis() / 1000;
        addReading(seconds, type, value);
    }

    /**
     * Adds a reading of the given type for the given time.
     * @param time Time of the reading.
     * @param type Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final long time, final ReadingType type, final String value) {
        final List<Reading> readingsList = getReadingsList(time);
        readingsList.add(new Reading(type, value));
    }

    private List<Reading> getReadingsList(final long time) {
        final Long key = readings.floorKey(time);
        if (key == null || time > key + delta) {
            final List<Reading> readingsList = new ArrayList<>();
            readings.put(time, readingsList);
            return readingsList;
        } else {
            return readings.get(key);
        }
    }

    /**
     * Publishes all the data and clears the reading list if publishing was successful.
     */
    public void publish() {
        if (readings.isEmpty()) {
            logger.info("No new readings. Not publishing.");
            return;
        }

        final String data = getPublishingData();
        try {
            publishingService.publish(data);
            readings.clear();
            logger.info("Publish successful. Readings list cleared");
        } catch (Exception e) {
            logger.error("Publishing data failed.", e);
        }
    }

    private String getPublishingData() {
        final StringBuilder data = new StringBuilder();
        data.append(RTC).append(System.currentTimeMillis() / 1000).append(MESSAGE_END);
        data.append(MESSAGE_START);
        boolean isFirstTimeInterval = true;
        for (final Long time :  readings.keySet()) {
            if (!isFirstTimeInterval) {
                data.append(TIME_DELIMITER);
            }
            isFirstTimeInterval = false;
            data.append(TIME_PREFIX).append(PREFIX_DELIMITER).append(time).append(READING_DELIMITER);

            boolean isFirstReading = true;
            for (Reading reading : readings.get(time)) {
                if (!isFirstReading) {
                    data.append(READING_DELIMITER);
                }
                isFirstReading = false;
                data.append(reading.getType().getPrefix()).append(PREFIX_DELIMITER).append(reading.getValue());
            }
        }
        data.append(MESSAGE_END);
        return data.toString();
    }

}
