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

import com.wolkabout.wolk.model.Feed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryPersistence implements Persistence {

    private final Queue<Feed> store = new LinkedBlockingQueue<>();

    @Override
    public void addFeed(Feed feed) {
        store.add(feed);
    }

    @Override
    public void addFeeds(Collection<Feed> feeds) {
        store.addAll(feeds);
    }

    @Override
    public Feed poll() {
        return store.poll();
    }

    @Override
    public List<Feed> getAll() {
        final ArrayList<Feed> feeds = new ArrayList<>(store);
        store.clear();
        return feeds;
    }

    @Override
    public void remove(Feed feed) {
        store.remove(feed);
    }

    @Override
    public void removeAll() {
        store.clear();
    }

}
