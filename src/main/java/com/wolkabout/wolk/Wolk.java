/*
 * Copyright 2016 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wolkabout.wolk;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Wolk {

    private final PublishingService publishingService;
    private final ReadingsBuffer readingsBuffer = new ReadingsBuffer();

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> publishTask;
    private Logger logger = new Logger() {};

    public Wolk(final Device device) {
        publishingService = new PublishingService(device);
    }

    /**
     * Sets the logging mechanism for the library.
     * @param logger Platform specific implementation of the logging mechanism.
     */
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets a time delta to be used when comparing reading times.
     * If two intervals are within the delta range, the lower time will be set for both readings.
     * Default is 0.
     * @param delta Time interval in seconds.
     */
    public void setTimeDelta(final int delta) {
        readingsBuffer.setDelta(delta);
    }

    /**
     * Starts publishing readings on a given interval.
     * @param interval Time interval in seconds to elapse between two publish attempts.
     */
    public void startAutoPublishing(final int interval) {
        publishTask = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                publish();
                if (!publishTask.isCancelled()) {
                    publishTask = executorService.schedule(this, interval, TimeUnit.SECONDS);
                }
            }
        }, interval, TimeUnit.SECONDS);
    }

    /**
     * Cancels a started automatic publishing task.
     */
    public void stopAutoPublishing() {
        publishTask.cancel(true);
    }

    /**
     * Adds a reading of the given type for the current time.
     * @param type Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final ReadingType type, final String value) {
        readingsBuffer.addReading(type, value);
    }

    /**
     * Adds a reading of the given type for the given time.
     * @param time Time of the reading.
     * @param type Type of the reading.
     * @param value Value of the reading.
     */
    public void addReading(final long time, final ReadingType type, final String value) {
        readingsBuffer.addReading(time, type, value);
    }

    /**
     * Publishes all the data and clears the reading list if publishing was successful.
     */
    public void publish() {
        if (readingsBuffer.isEmpty()) {
            logger.info("No new readings. Not publishing.");
            return;
        }

        try {
            publishingService.publish(readingsBuffer.getFormattedData());
            readingsBuffer.removePublishedReadings();
            logger.info("Publish successful. Readings list trimmed.");
        } catch (Exception e) {
            logger.error("Publishing data failed.", e);
        }
    }

    /**
     * Set a new host address.
     * @param host A new host address.
     */
    public void setHost(String host) {
        publishingService.setHOST(host);
    }

}
