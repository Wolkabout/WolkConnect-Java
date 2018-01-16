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

import com.wolkabout.wolk.filetransfer.FileTransferPacketRequest;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdate;
import com.wolkabout.wolk.firmwareupdate.FirmwareUpdateStatus;

import java.util.ArrayList;
import java.util.List;

public class FirmwareUpdateStatusAggregator implements FirmwareUpdate.Listener {

    private final List<FirmwareUpdateStatus> firmwareUpdateStatuses = new ArrayList<>();

    public List<FirmwareUpdateStatus> waitFor(int statusCount) throws InterruptedException {
        while(firmwareUpdateStatuses.size() < statusCount) {
            Thread.sleep(50);
        }

        return firmwareUpdateStatuses;
    }

    public void clear() {
        firmwareUpdateStatuses.clear();
    }

    @Override
    public void onStatus(FirmwareUpdateStatus status) {
        firmwareUpdateStatuses.add(status);
    }

    @Override
    public void onFilePacketRequest(FileTransferPacketRequest request) {
    }
}
