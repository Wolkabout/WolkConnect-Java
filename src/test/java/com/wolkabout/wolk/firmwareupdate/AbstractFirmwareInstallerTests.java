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
package com.wolkabout.wolk.firmwareupdate;

import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateError;
import com.wolkabout.wolk.firmwareupdate.model.FirmwareUpdateStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractFirmwareInstallerTests {

    @Mock
    FirmwareUpdateProtocol protocolMock;

    @Mock
    FirmwareInstaller installer;

    @Before
    public void setUp() {
        doCallRealMethod().when(installer).setFirmwareUpdateProtocol(any());
        installer.setFirmwareUpdateProtocol(protocolMock);
    }

    @Test
    public void checkPublishFirmwareVersion() {
        installer.publishFirmwareVersion("Firmware version");
        verify(protocolMock, times(1)).publishFirmwareVersion("Firmware version");
    }

    @Test
    public void checkDefaultOnInit() {
        doCallRealMethod().when(installer).onInstallCommandReceived(anyString());
        installer.onInstallCommandReceived("File name");
        verify(protocolMock, times(1)).sendStatusMessage(eq(FirmwareUpdateStatus.INSTALLATION));
    }

    @Test
    public void checkDefaultAbort() {
        doCallRealMethod().when(installer).onAbortCommandReceived();
        installer.onAbortCommandReceived();
        verify(protocolMock, times(1)).sendStatusMessage(eq(FirmwareUpdateStatus.ABORTED));
    }

    @Test
    public void checkDefaultErrorPublish() {
        installer.publishError(FirmwareUpdateError.UNSPECIFIED_ERROR);
        verify(protocolMock, times(1)).sendErrorMessage(FirmwareUpdateError.UNSPECIFIED_ERROR);
    }
}
