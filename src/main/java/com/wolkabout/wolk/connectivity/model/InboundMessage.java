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
package com.wolkabout.wolk.connectivity.model;

import java.nio.charset.StandardCharsets;

public class InboundMessage {
    private final String channel;
    private final byte[] payload;

    public InboundMessage(String channel, byte[] payload) {
        this.channel = channel;
        this.payload = payload.clone();
    }

    public String getPayload() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    public byte[] getBinaryPayload() {
        return payload;
    }

    public String getChannel() {
        return channel;
    }
}
