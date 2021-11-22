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

import com.wolkabout.wolk.firmwareupdate.ScheduledFirmwareUpdate;
import com.wolkabout.wolk.model.OutboundDataMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;

import static com.wolkabout.wolk.Wolk.WOLK_DEMO_CA;
import static com.wolkabout.wolk.Wolk.WOLK_DEMO_URL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WolkTest {

    @Mock
    ScheduledFirmwareUpdate scheduledFirmwareUpdateMock;

    Wolk wolk;

    @Before
    public void init() throws NoSuchFieldException {
        wolk = Wolk.builder(OutboundDataMode.PUSH)
                .mqtt()
                .host(WOLK_DEMO_URL)
                .sslCertification(WOLK_DEMO_CA)
                .deviceKey("device_key")
                .password("password")
                .build()
                .build();

        FieldSetter.setField(wolk, wolk.getClass().getDeclaredField("scheduledFirmwareUpdate"), scheduledFirmwareUpdateMock);
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

    @Test
    public void firmwareUpdateCheckTime() {

        wolk.onFirmwareUpdateCheckTime(5);

        ArgumentCaptor<LocalTime> argument = ArgumentCaptor.forClass(LocalTime.class);
        verify(scheduledFirmwareUpdateMock, times(1)).setTimeAndReschedule(argument.capture());
        assertEquals(argument.getValue().getHour(), 5);
    }

    @Test
    public void firmwareUpdateCheckTimeUnderRange() {

        wolk.onFirmwareUpdateCheckTime(-1);

        verify(scheduledFirmwareUpdateMock, times(1)).setTimeAndReschedule(null);
    }

    @Test
    public void firmwareUpdateCheckTimeOverRange() {

        wolk.onFirmwareUpdateCheckTime(25);

        verify(scheduledFirmwareUpdateMock, times(1)).setTimeAndReschedule(null);
    }
}
