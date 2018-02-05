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

import java.util.*;

public class InMemoryPersistence implements Persistence {
    private final HashMap<String, List<SensorReading>> readings = new LinkedHashMap<>();
    private final HashMap<String, List<Alarm>> alarms = new LinkedHashMap<>();
    private final HashMap<String, ActuatorStatus> actuatorStatuses = new LinkedHashMap<>();


    @Override
    public boolean putSensorReading(String key, SensorReading reading) {
        if (readings.get(key) == null) {
            readings.put(key, new ArrayList<SensorReading>());
        }

        return readings.get(key).add(reading);
    }

    @Override
    public List<SensorReading> getSensorReadings(String key, int count) {
        if (!readings.containsKey(key)) {
            return new ArrayList<>();
        }

        final List<SensorReading> readingsByKey = readings.get(key);
        return readingsByKey.subList(0, Math.min(readingsByKey.size(), count));
    }


    @Override
    public void removeSensorReadings(String key, int count) {
        if (!readings.containsKey(key)) {
            return;
        }

        final List<SensorReading> readingsByKey = readings.get(key);
        if (readingsByKey.size() <= count) {
            readings.remove(key);
        } else {
            readings.put(key, readingsByKey.subList(count, readingsByKey.size()));
        }
    }

    @Override
    public List<String> getSensorReadingsKeys() {
        return new ArrayList<>(readings.keySet());
    }

    @Override
    public boolean putAlarm(String key, Alarm alarm) {
        if (alarms.get(key) == null) {
            alarms.put(key, new ArrayList<Alarm>());
        }

        alarms.get(key).add(alarm);
        return true;
    }

    @Override
    public List<Alarm> getAlarms(String key, int count) {
        if (!alarms.containsKey(key)) {
            return Collections.emptyList();
        }

        final List<Alarm> alarmsByKey = alarms.get(key);
        return alarmsByKey.subList(0, Math.min(alarmsByKey.size(), count));
    }

    @Override
    public void removeAlarms(String key, int count) {
        if (!alarms.containsKey(key)) {
            return;
        }

        final List<Alarm> alarmsByKey = alarms.get(key);
        if (alarmsByKey.size() <= count) {
            alarms.remove(key);
        } else {
            alarms.put(key, alarmsByKey.subList(count, alarmsByKey.size()));
        }
    }

    @Override
    public List<String> getAlarmsKeys() {
        return new ArrayList<>(alarms.keySet());
    }

    @Override
    public boolean putActuatorStatus(String key, ActuatorStatus actuatorStatus) {
        return actuatorStatuses.put(key, actuatorStatus) != null;
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
        return readings.isEmpty() && actuatorStatuses.isEmpty();
    }
}
