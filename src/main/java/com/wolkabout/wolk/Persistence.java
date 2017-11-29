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

import java.util.List;

/**
 * A storage designed for holding elements in persistent store prior to publishing to WolkAbout IoT Platform.
 * <p>
 * Multiple {@link Reading}s can be stored under the same key.
 * Multiple {@link Alarm}s can be stored under the same key.
 * Single {@link ActuatorStatus} can be stored under one key.
 * Implementation storing/retrieving strategy must be FIFO.
 */
public interface Persistence {
    /**
     * Inserts the {@link Reading}.
     *
     * @param key     with which {@link Reading} should be associated
     * @param reading to be inserted
     * @return {@code true} if successful, or {@code false} if
     * element can not be inserted
     */
    boolean putReading(String key, Reading reading);

    /**
     * Retrieves, first {@code count} {@link Reading}s of this storage, associated with given {@code key}
     * or returns empty {@code List<Reading>} if this storage is empty.
     *
     * @param key   with which {@link Reading} should be associated
     * @param count number of items to peek
     * @return {@code List<Reading>} containing {@code count} {@link Reading}s starting from the head,
     * or returns less than {@code count} {@link Reading}s if this storage does not have requested number of elements
     */
    List<Reading> getReadings(String key, int count);

    /**
     * Removes first {@code count} {@link Reading}s of this storage, associated with given {@code key}.
     *
     * @param key   of the {@link Reading}s
     * @param count number of items to remove
     */
    void removeReadings(String key, int count);

    /**
     * Returns {@code List<String>} of {@link Reading} keys contained in this storage.
     *
     * @return {@code List<String>} containing keys, or empty {@code List<String>} if no {@link Reading}s are present.
     */
    List<String> getReadingsKeys();

    /**
     * Inserts the {@link Alarm}.
     *
     * @param key   with which {@link Alarm} should be associated
     * @param alarm to be inserted
     * @return {@code true} if successful, or {@code false} if
     * element can not be inserted
     */
    boolean putAlarm(String key, Alarm alarm);

    /**
     * Retrieves, first {@code count} {@link Alarm}s of this storage, associated with given {@code key}
     * or returns empty {@code List<Alarm>} if this storage is empty.
     *
     * @param key   with which {@link Alarm} should be associated
     * @param count number of items to peek
     * @return {@code List<Alarm>} containing {@code count} {@link Alarm}s starting from the head,
     * or returns less than {@code count} {@link Alarm}s if this storage does not have requested number of elements
     */
    List<Alarm> getAlarms(String key, int count);

    /**
     * Removes first {@code count} {@link Alarm}s of this storage, associated with given {@code key}.
     *
     * @param key   of the {@link Alarm}s
     * @param count number of items to remove
     */
    void removeAlarms(String key, int count);

    /**
     * Returns {@code List<String>} of {@link Alarm} keys contained in this storage
     *
     * @return {@code List<String>} containing keys, or empty {@code List<String>} if no {@link Alarm}s are present.
     */
    List<String> getAlarmsKeys();

    /**
     * Inserts the {@link ActuatorStatus}.
     *
     * @param key            with which {@link ActuatorStatus} should be associated.
     * @param actuatorStatus to be inserted
     * @return {@code true} if successful, or {@code false} if
     * element can not be inserted
     */
    boolean putActuatorStatus(String key, ActuatorStatus actuatorStatus);

    /**
     * Retrieves, {@link ActuatorStatus} of this storage, associated with given {@code key}.
     *
     * @param key of the {@link ActuatorStatus}.
     * @return {@link ActuatorStatus}, or {@code null} if this storage does not contain {@link ActuatorStatus} for given {@code key}
     */
    ActuatorStatus getActuatorStatus(String key);

    /**
     * Removes {@link ActuatorStatus} from this storage, associated with given {@code key}.
     *
     * @param key with which {@link Reading} should be associated.
     */
    void removeActuatorStatus(String key);

    /**
     * Returns {@code List<String>} of {@link ActuatorStatus} keys contained in this storage.
     *
     * @return {@code List<String>} containing keys, or empty {@code List<String>} if no {@link ActuatorStatus}es are present.
     */
    List<String> getActuatorStatusesKeys();

    /**
     * Returns {@code true} if this storage contains no {@link Reading}s, {@link ActuatorStatus}es and {@link Alarm}s associated with any key.
     *
     * @return {@code true} if this storage contains no {@link Reading}s, {@link ActuatorStatus}es and {@link Alarm}s associated with any key
     */
    boolean isEmpty();
}
