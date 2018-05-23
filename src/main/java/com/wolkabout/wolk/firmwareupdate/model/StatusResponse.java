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

package com.wolkabout.wolk.firmwareupdate.model;

import java.util.Objects;

public class StatusResponse {

    private FirmwareStatus status;
    private UpdateError error;

    public FirmwareStatus getStatus() {
        return status;
    }

    public void setStatus(FirmwareStatus status) {
        this.status = status;
    }

    public UpdateError getError() {
        return error;
    }

    public void setError(UpdateError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "status=" + status +
                ", error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatusResponse)) return false;
        StatusResponse that = (StatusResponse) o;
        return error == that.error &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, error);
    }
}
