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

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

class ReadingSerializer {
    private static final String TIME_PREFIX = "R";
    private static final String RTC = "RTC ";
    private static final String MESSAGE_START = "READINGS ";
    private static final String PREFIX_DELIMITER = ":";
    private static final String READING_DELIMITER = ",";
    private static final String TIME_DELIMITER = "|";
    private static final String MESSAGE_END = ";";

    private static final Logger LOG = LoggerFactory.getLogger(ReadingSerializer.class);

    private String topic;
    private String payload;
    private int numSerializedReadings;

    private ReadingSerializer(final Device device, final List<Reading> readings) {
        final Protocol protocol = device.getProtocol();

        switch (protocol) {
            default:
                LOG.warn("Unsupported protocol " + protocol + " defaulting to JSON_SINGLE");
            case JSON_SINGLE:
                serializeJsonSingle(device, readings);
                break;

            case WOLK_SENSE:
                serializeWolksense(device, readings);
                break;
        }
    }

    private void serializeJsonSingle(final Device device, final List<Reading> readings) {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Reading.class, new ReadingJsonSerializer());
        final Gson gson = builder.create();

        topic = device.getProtocol().getReadingsTopic() + device.getDeviceKey() + "/" + readings.get(0).getReference();
        payload = gson.toJson(readings.get(0));
        numSerializedReadings = 1;
    }

    private void serializeWolksense(final Device device, final List<Reading> readings) {
        final NavigableMap<Long, List<Reading>> readingsByTime = new TreeMap<>();

        for (final Reading reading : readings) {
            final long utc = reading.getUtc();
            final Long key = readingsByTime.floorKey(utc);

            if (key == null) {
                final List<Reading> readingsList = new ArrayList<>();
                readingsByTime.put(utc, readingsList);
            }

            final List<Reading> readingsList = readingsByTime.get(utc);
            readingsList.add(reading);
        }

        final StringBuilder data = new StringBuilder();
        data.append(RTC).append(System.currentTimeMillis() / 1000).append(MESSAGE_END);
        data.append(MESSAGE_START);
        for (final long time : readingsByTime.keySet()) {
            data.append(TIME_PREFIX).append(PREFIX_DELIMITER).append(time).append(READING_DELIMITER);

            boolean isFirstReading = true;
            for (Reading reading : readingsByTime.get(time)) {
                if (!isFirstReading) {
                    data.append(READING_DELIMITER);
                }
                isFirstReading = false;
                data.append(reading.getReference()).append(PREFIX_DELIMITER).append(reading.getValue());
            }

            if (time != readingsByTime.lastKey()) {
                data.append(TIME_DELIMITER);
            }
        }
        data.append(MESSAGE_END);

        topic = device.getProtocol().getReadingsTopic() + device.getDeviceKey();
        payload =  data.toString();
        numSerializedReadings = readings.size();
    }

    private class ReadingJsonSerializer implements JsonSerializer<Reading> {
        @Override
        public JsonElement serialize(Reading reading, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("utc", reading.getUtc());
            jsonObject.addProperty("data", reading.getValue());
            return jsonObject;
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public int getNumberOfSerializedReadings() {
        return numSerializedReadings;
    }

    public static class Factory {
        private final Device device;

        public Factory(final Device device) {
            this.device = device;
        }

        public ReadingSerializer newReadingSerializer(List<Reading> readings) {
            return new ReadingSerializer(device, readings);
        }
    }
}
