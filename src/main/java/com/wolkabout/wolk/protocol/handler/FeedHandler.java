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
package com.wolkabout.wolk.protocol.handler;

import com.wolkabout.wolk.model.Feed;

import java.util.Collection;

public abstract class FeedHandler {

    /**
     * Called when feeds are received.
     *
     * @param feeds Collection of key-value pair of references and values.
     */
    public abstract void onFeedsReceived(Collection<Feed> feeds);

    /**
     * Called when feed is requested by server.
     *
     * @return Feed reference.
     */
    public abstract Feed getFeedValue(String reference);
}
