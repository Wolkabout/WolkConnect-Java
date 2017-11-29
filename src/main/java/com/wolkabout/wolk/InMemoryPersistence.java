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

import java.util.*;

public class InMemoryPersistence implements Persistence {
    private final HashMap<String, List<Reading>> readings = new LinkedHashMap<>();
    private final HashMap<String, List<Alarm>> alarms = new LinkedHashMap<>();
    private final HashMap<String, ActuatorStatus> actuatorStatuses = new LinkedHashMap<>();

    private List<Reading> getOrCreateReadingsByKey(String key) {
        if (!readings.containsKey(key)) {
            readings.put(key, new ArrayList<Reading>());
        }

        return readings.get(key);
    }

    private List<Alarm> getOrCreateAlarmsByKey(String key) {
        if (!alarms.containsKey(key)) {
            alarms.put(key, new ArrayList<Alarm>());
        }

        return alarms.get(key);
    }

    @Override
    public boolean putReading(String key, Reading reading) {
        getOrCreateReadingsByKey(key).add(reading);
        return true;
    }

    @Override
    public List<Reading> getReadings(String key, int count) {
        if (!readings.containsKey(key)) {
            return new ArrayList<>();
        }

        final List<Reading> readingsByKey = getOrCreateReadingsByKey(key);
        return readingsByKey.subList(0, Math.min(readingsByKey.size(), count));
    }


    @Override
    public void removeReadings(String key, int count) {
        if (!readings.containsKey(key)) {
            return;
        }

        final List<Reading> readingsByKey = getOrCreateReadingsByKey(key);
        readings.put(key, readingsByKey.subList(Math.min(readingsByKey.size(), count), readingsByKey.size()));
    }

    @Override
    public List<String> getReadingsKeys() {
        List<String> keys = new ArrayList<>();
        for (final String key : readings.keySet()) {
            if (!readings.get(key).isEmpty()) {
                keys.add(key);
            }
        }

        return keys;
    }

    @Override
    public boolean putAlarm(String key, Alarm alarm) {
        getOrCreateAlarmsByKey(key).add(alarm);
        return true;
    }

    @Override
    public List<Alarm> getAlarms(String key, int count) {
        if (!readings.containsKey(key)) {
            return Collections.emptyList();
        }

        final List<Alarm> alarmsByKey = getOrCreateAlarmsByKey(key);
        return alarmsByKey.subList(0, Math.min(alarmsByKey.size(), count));
    }

    @Override
    public void removeAlarms(String key, int count) {
        if (!alarms.containsKey(key)) {
            return;
        }

        final List<Alarm> alarmsByKey = getOrCreateAlarmsByKey(key);
        alarms.put(key, alarmsByKey.subList(Math.min(alarmsByKey.size(), count), alarmsByKey.size()));
    }

    @Override
    public List<String> getAlarmsKeys() {
        List<String> keys = new ArrayList<>();
        for (final String key : alarms.keySet()) {
            if (!alarms.get(key).isEmpty()) {
                keys.add(key);
            }
        }

        return keys;
    }

    @Override
    public boolean putActuatorStatus(String key, ActuatorStatus actuatorStatus) {
        actuatorStatuses.put(key, actuatorStatus);
        return true;
    }

    @Override
    public ActuatorStatus getActuatorStatus(String key) {
        return actuatorStatuses.get(key);
    }

    @Override
    public void removeActuatorStatus(String key) {
        actuatorStatuses.remove(key);
    }

    @Override
    public List<String> getActuatorStatusesKeys() {
        return new ArrayList<>(actuatorStatuses.keySet());
    }

    @Override
    public boolean isEmpty() {
        for (final String key : getReadingsKeys()) {
            if (!readings.get(key).isEmpty()) {
                return false;
            }
        }

        return actuatorStatuses.isEmpty();
    }
}
