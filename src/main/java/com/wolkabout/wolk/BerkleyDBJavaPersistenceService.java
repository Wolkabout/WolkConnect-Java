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
import com.sleepycat.je.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BerkleyDBJavaPersistenceService implements PersistenceService {
    private static final String readingsDbName = "readings";

    private static final Logger LOG = LoggerFactory.getLogger(BerkleyDBJavaPersistenceService.class);

    private final Environment dbEnvironment;
    private final Database readingsDb;

    private final Gson gson;

    static private class KeyComparator implements Comparator<byte[]>, Serializable {
        public int compare(byte[] first, byte[] second) {
            return new BigInteger(first).compareTo(new BigInteger(second));
        }
    }

    public BerkleyDBJavaPersistenceService(final File persistentStorePath) {
        LOG.info("Initializing");

        if (!persistentStorePath.exists() && persistentStorePath.mkdir()) {
            LOG.error("Could not create persistent store in " + persistentStorePath);
        }

        final EnvironmentConfig dbEnvironmentConfig = new EnvironmentConfig();
        dbEnvironmentConfig.setTransactional(false);
        dbEnvironmentConfig.setAllowCreate(true);
        dbEnvironment = new Environment(persistentStorePath, dbEnvironmentConfig);

        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setDeferredWrite(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setBtreeComparator(new KeyComparator());
        readingsDb = dbEnvironment.openDatabase(null, readingsDbName, dbConfig);

        gson = new Gson();

        LOG.debug("Readings DB contains " + readingsDb.count() + " persisted items");
    }

    public BerkleyDBJavaPersistenceService(final String persistentStorePath) {
        this(new File(persistentStorePath));
    }

    @Override
    public boolean offer(Reading reading) {
        LOG.debug("Offer " + reading);

        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();

        try (Cursor cursor = readingsDb.openCursor(null, null)) {
            cursor.getLast(key, data, LockMode.RMW);

            final BigInteger prevKeyValue = key.getData() == null ? BigInteger.valueOf(-1) : new BigInteger(key.getData());
            final BigInteger newKeyValue = prevKeyValue.add(BigInteger.ONE);

            try {
                final DatabaseEntry newKey = new DatabaseEntry(newKeyValue.toByteArray());
                final DatabaseEntry newData = new DatabaseEntry(gson.toJson(reading).getBytes("UTF-8"));

                final boolean isSuccessful = readingsDb.put(null, newKey, newData) == OperationStatus.SUCCESS;
                readingsDb.sync();
                return isSuccessful;
            } catch (Exception e) {
                LOG.debug("Offer reading failed", e);
                return false;
            }
        }
    }

    @Override
    public Reading peekReading() {
        LOG.debug("Peek reading");

        final List<Reading> readings = peekReadings(1);
        return readings.isEmpty() ? null : readings.get(0);
    }

    @Override
    public List<Reading> peekReadings(int count) {
        LOG.debug("Peek readings: count=" + count);

        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();

        List<Reading> readings = new ArrayList<>();

        try (Cursor cursor = readingsDb.openCursor(null, null)) {
            for (long i = 0; i < count; ++i) {
                final boolean operationSuccessful = cursor.getNext(key, data, LockMode.RMW) == OperationStatus.SUCCESS;
                if (!operationSuccessful || data.getData() == null) {
                    break;
                }

                final String json = new String(data.getData(), "UTF-8");
                final Reading reading = gson.fromJson(json, Reading.class);
                readings.add(reading);
            }
        } catch (Exception e) {
            LOG.debug("Peek readings failed", e);
        }

        return readings;
    }

    @Override
    public Reading pollReading() {
        LOG.debug("Poll reading");

        final List<Reading> readings = pollReadings(1);
        return readings.isEmpty() ? null : readings.get(0);
    }

    @Override
    public List<Reading> pollReadings(int count) {
        LOG.debug("Poll readings: count=" + count);

        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();

        List<Reading> readings = new ArrayList<>();

        try (Cursor cursor = readingsDb.openCursor(null, null)) {
            for (long i = 0; i < count; ++i) {
                final boolean operationSuccessful = cursor.getNext(key, data, LockMode.RMW) == OperationStatus.SUCCESS;
                if (!operationSuccessful || data.getData() == null) {
                    break;
                }
                cursor.delete();

                final String json = new String(data.getData(), "UTF-8");
                final Reading reading = gson.fromJson(json, Reading.class);
                readings.add(reading);
            }
        } catch (Exception e) {
            LOG.debug("Poll readings failed", e);
        }

        return readings;
    }

    @Override
    public boolean isEmpty() {
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry();

        try (Cursor cursor = readingsDb.openCursor(null, null)) {
            cursor.getFirst(key, data, LockMode.RMW);
            return data.getData() == null;
        } catch (Exception e) {
            LOG.error("Unable to check if readings DB has any records", e);
            return true;
        }
    }
}