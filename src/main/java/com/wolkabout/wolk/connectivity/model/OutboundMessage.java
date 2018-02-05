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
package com.wolkabout.wolk.connectivity.model;

public class OutboundMessage {
    private final String payload;
    private final String channel;
    private final int serializedItemsCount;

    public OutboundMessage(String payload, String channel) {
        this.payload = payload;
        this.channel = channel;
        this.serializedItemsCount = -1;
    }

    public OutboundMessage(String payload, String channel, int serializedItemsCount) {
        this.payload = payload;
        this.channel = channel;
        this.serializedItemsCount = serializedItemsCount;
    }

    public String getPayload() {
        return payload;
    }

    public String getChannel() {
        return channel;
    }

    public int getSerializedItemsCount() {
        return serializedItemsCount;
    }
}
