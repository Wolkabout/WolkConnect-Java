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

import com.wolkabout.wolk.Wolk;

import java.lang.ref.WeakReference;

public abstract class FirmwareInstaller {

    private WeakReference<Wolk> wolk;

    protected Wolk getWolk() {
        return wolk.get();
    }

    public void setWolk(Wolk wolk) {
        if (this.wolk != null) {
            throw new IllegalStateException("Wolk instance already set.");
        }

        this.wolk = new WeakReference<>(wolk);
    }

    public abstract void onFileReady(String fileName, boolean autoInstall, byte[] bytes);

    public abstract void onInstallCommandReceived();

    public abstract void onAbortCommandReceived();
}
