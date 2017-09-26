/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

class ReadingsBuffer {

    private static final String TIME_PREFIX = "R";
    private static final String RTC = "RTC ";
    private static final String MESSAGE_START = "READINGS ";
    private static final String PREFIX_DELIMITER = ":";
    private static final String READING_DELIMITER = ",";
    private static final String TIME_DELIMITER = "|";
    private static final String MESSAGE_END = ";";

    private final ConcurrentNavigableMap<Long, List<Reading>> readingsByTime = new ConcurrentSkipListMap<>();

    private final ConcurrentHashMap<String, List<Reading>> readingsBySensor = new ConcurrentHashMap<>();

    private final List<Long> publishedTimes = new ArrayList<>();

    private int delta = 0;

    private Protocol protocol;

    public ReadingsBuffer(Protocol protocol) {
        this.protocol = protocol;
    }

    boolean isEmpty() {
        return readingsByTime.isEmpty() && readingsBySensor.isEmpty();
    }

    void removePublishedReadings() {
        for (final long publishedTime : publishedTimes) {
            readingsByTime.remove(publishedTime);
        }
        publishedTimes.clear();
        readingsBySensor.clear();
    }

    void setDelta(final int delta) {
        this.delta = delta;
    }

    void addReading(final String ref, final String value) {
        if (protocol == Protocol.WolkSense) {
            final long seconds = System.currentTimeMillis() / 1000;
            final List<Reading> readingsList = getReadingsList(seconds);
            readingsList.add(new Reading(ReadingType.fromPrefix(ref), value));
        } else {
            List<Reading> readings = readingsBySensor.get(ref);
            if (readings == null) {
                readings = new ArrayList<>();
                readingsBySensor.put(ref, readings);
            }
            readings.add(new Reading(ref, value));
        }
    }

    public List<String> getReferences() {
        return Collections.list(readingsBySensor.keys());
    }

    private List<Reading> getReadingsList(final long time) {
        final Long key = readingsByTime.floorKey(time);
        if (key == null || time > key + delta) {
            final List<Reading> readingsList = new ArrayList<>();
            readingsByTime.put(time, readingsList);
            return readingsList;
        } else {
            return readingsByTime.get(key);
        }
    }

    String getJsonFormattedData(String ref) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Reading.class, new Reading.ReadingSerializer());
        Gson gson = builder.create();
        final List<Reading> src = readingsBySensor.get(ref);
        if (src.size() == 1) {
            return gson.toJson(src.get(0));
        }
        return gson.toJson(src);
    }

    String getFormattedData() {
        final StringBuilder data = new StringBuilder();
        data.append(RTC).append(System.currentTimeMillis() / 1000).append(MESSAGE_END);
        data.append(MESSAGE_START);
        for (final long time : readingsByTime.keySet()) {
            publishedTimes.add(time);
            data.append(TIME_PREFIX).append(PREFIX_DELIMITER).append(time).append(READING_DELIMITER);

            appendReadings(data, time);

            if (time != readingsByTime.lastKey()) {
                data.append(TIME_DELIMITER);
            }
        }
        data.append(MESSAGE_END);
        return data.toString();
    }

    private void appendReadings(StringBuilder data, long time) {
        boolean isFirstReading = true;
        for (Reading reading : readingsByTime.get(time)) {
            if (!isFirstReading) {
                data.append(READING_DELIMITER);
            }
            isFirstReading = false;
            data.append(reading.getType().getPrefix()).append(PREFIX_DELIMITER).append(reading.getValue());
        }
    }
}
