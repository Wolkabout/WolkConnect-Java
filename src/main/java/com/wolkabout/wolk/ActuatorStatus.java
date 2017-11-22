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

/**
 * Contains the status of the Actuator.
 */
public class ActuatorStatus {
    public enum Status {READY, BUSY, ERROR}

    private String ref;
    private Status status;
    private String value;

    public ActuatorStatus(Status status, String value) {
        this.status = status;
        this.value = value;
    }

    ActuatorStatus(Status status, String value, String ref) {
        this(status, value);
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public Status getStatus() {
        return status;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ActuatorStatus {" +
                "ref='" + ref + '\'' +
                ", status='" + status + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
