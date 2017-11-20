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
 * A collection designed for holding elements in persistent store prior to processing.
 *
 * Must be implemented to follow FIFO element storing/retrieving strategy.
 */
public interface PersistenceService {
    /**
     * Inserts the {@link Reading} at the tail of queue.
     *
     * @param reading to be inserted
     * @return {@code true} if successful, or {@code false} if
     *         element can not be inserted
     */
    boolean offer(Reading reading);

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns null if this queue is empty.
     *
     * @return the head of this queue, or null if this queue is empty
     */
    Reading peekReading();

    /**
     * Retrieves, but does not remove, first {@code count} {@link Reading}s of this queue,
     * or returns empty @{link List<Reading></Reading>} if this queue is empty.
     *
     * @param count number of items to peek
     * @return @{link List<Reading>} containing {@code count} {@link Reading}s starting from the head,
     *         or returns less {@code count} {@link Reading}s if this queue does not have requested number of elements
     */
    List<Reading> peekReadings(int count);

    /**
     * Retrieves and removes the head of this queue,
     * or returns null if this queue is empty.
     *
     * @return the head of this queue, or null if this queue is empty
     */
    Reading pollReading();

    /**
     * Retrieves and removes first {@code count} {@link Reading}s of this queue,
     * or returns empty @{link List<Reading></Reading>} if this queue is empty.
     *
     * @param count number of items to poll
     * @return @{link List<Reading>} containing {@code count} {@link Reading}s starting from the head,
     *         or returns less {@code count} {@link Reading}s if this queue does not have requested number of elements
     */
    List<Reading> pollReadings(int count);

    /**
     * Returns true if this queue contains no elements.
     *
     * @return {@code true} if there is data in persistent storage,
     *         or @{code false} if there is not data in persistent storage
     */
    boolean isEmpty();
}
