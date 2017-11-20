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

class OutboundMessage {
    private final String payload;
    private final String topic;
    private final int serializedItemsCount;

    public OutboundMessage(final String payload, final String topic, final int serializedItemsCount) {
        this.payload = payload;
        this.topic = topic;
        this.serializedItemsCount = serializedItemsCount;
    }

    public String getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }

    public int getSerializedItemsCount() {
        return serializedItemsCount;
    }
}
