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

import com.wolkabout.wolk.model.Reading;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryPersistence implements Persistence {

    private final Queue<Reading> store = new ConcurrentLinkedQueue<>();

    @Override
    public void addReading(Reading reading) {
        store.add(reading);
    }

    @Override
    public void addReadings(Collection<Reading> readings) {
        store.addAll(readings);
    }

    @Override
    public Reading poll() {
        return store.poll();
    }

    @Override
    public List<Reading> getAll() {
        return new ArrayList<>(store);
    }

    @Override
    public void remove(Reading reading) {
        store.remove(reading);
    }

    @Override
    public void removeAll() {
        store.clear();
    }

}
