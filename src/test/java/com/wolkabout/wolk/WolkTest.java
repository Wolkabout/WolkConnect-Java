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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.wolkabout.wolk.firmwareupdate.ScheduledFirmwareUpdate;
import com.wolkabout.wolk.model.OutboundDataMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import static com.cronutils.model.field.expression.FieldExpressionFactory.*;
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

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(always())
                .withDoM(always())
                .withMonth(always())
                .withDoW(questionMark())
                .withHour(on(5))
                .withMinute(on(0))
                .withSecond(on(0))
                .instance();

        wolk.onFirmwareUpdateCheckTime(cron.asString());

        ArgumentCaptor<Cron> argument = ArgumentCaptor.forClass(Cron.class);
        verify(scheduledFirmwareUpdateMock, times(1)).setTimeAndReschedule(argument.capture());
        assertEquals(argument.getValue().asString(), cron.asString());
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
