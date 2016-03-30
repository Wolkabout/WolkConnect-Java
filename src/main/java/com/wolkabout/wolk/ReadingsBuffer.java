package com.wolkabout.wolk;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

class ReadingsBuffer {

    private static final String TIME_PREFIX = "R";
    private static final String RTC = "RTC ";
    private static final String MESSAGE_START = "READINGS ";
    private static final String PREFIX_DELIMITER = ":";
    private static final String READING_DELIMITER = ",";
    private static final String TIME_DELIMITER = "|";
    private static final String MESSAGE_END = ";";

    private final NavigableMap<Long, List<Reading>> readings= new TreeMap<>();
    private final List<Long> publishedTimes = new ArrayList<>();

    private int delta = 0;

    boolean isEmpty() {
        return readings.isEmpty();
    }

    void removePublishedReadings() {
        for (final long publishedTime : publishedTimes) {
            readings.remove(publishedTime);
        }
        publishedTimes.clear();
    }

    void setDelta(final int delta) {
        this.delta = delta;
    }

    void addReading(final ReadingType type, final String value) {
        final long seconds = System.currentTimeMillis() / 1000;
        addReading(seconds, type, value);
    }

    void addReading(final long time, final ReadingType type, final String value) {
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

    String getFormattedData() {
        final StringBuilder data = new StringBuilder();
        data.append(RTC).append(System.currentTimeMillis() / 1000).append(MESSAGE_END);
        data.append(MESSAGE_START);
        for (final long time :  readings.keySet()) {
            publishedTimes.add(time);
            data.append(TIME_PREFIX).append(PREFIX_DELIMITER).append(time).append(READING_DELIMITER);

            appendReadings(data, time);

            if (time != readings.lastKey()) {
                data.append(TIME_DELIMITER);
            }
        }
        data.append(MESSAGE_END);
        return data.toString();
    }

    private void appendReadings(StringBuilder data, long time) {
        boolean isFirstReading = true;
        for (Reading reading : readings.get(time)) {
            if (!isFirstReading) {
                data.append(READING_DELIMITER);
            }
            isFirstReading = false;
            data.append(reading.getType().getPrefix()).append(PREFIX_DELIMITER).append(reading.getValue());
        }
    }
}
