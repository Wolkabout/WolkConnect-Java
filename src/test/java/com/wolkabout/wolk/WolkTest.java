/*
 * Copyright (c) 2021 WolkAbout Technology s.r.o.
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

import com.wolkabout.wolk.model.OutboundDataMode;
import org.junit.Test;

public class WolkTest {

    @Test
    public void connect() {
        Wolk wolk = Wolk.builder(OutboundDataMode.PUSH)
                .mqtt()
                .host("ssl://api-demo.wolkabout.com:8883")
                .sslCertification("ca.crt")
                .deviceKey("device_key")
                .password("password")
                .build()
                .build();
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void startPublishing() {
    }

    @Test
    public void stopPublishing() {
    }

    @Test
    public void publish() {
    }

    @Test
    public void addReading() {
    }

    @Test
    public void testAddReading() {
    }

    @Test
    public void testAddReading1() {
    }

    @Test
    public void addReadings() {
    }

    @Test
    public void addAlarm() {
    }

    @Test
    public void publishConfiguration() {
    }

    @Test
    public void publishActuatorStatus() {
    }

    @Test
    public void publishFirmwareVersion() {
    }

    @Test
    public void publishFileTransferStatus() {
    }

    @Test
    public void testPublishFileTransferStatus() {
    }

    @Test
    public void builder() {
    }
}
