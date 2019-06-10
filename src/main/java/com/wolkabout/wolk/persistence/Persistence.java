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
package com.wolkabout.wolk.persistence;

import com.wolkabout.wolk.model.Alarm;
import com.wolkabout.wolk.model.Reading;

import java.util.Collection;
import java.util.List;

public interface Persistence {

    void addReading(Reading reading);

    void addReadings(Collection<Reading> readings);

    Reading poll();

    List<Reading> getAll();

    void remove(Reading reading);

    void removeAll();

    void addAlarm(Alarm alarm);

    Alarm pollAlarms();

    List<Alarm> getAllAlarms();

    void removeAlarm(Alarm alarm);

    void removeAllAlarms();
}
