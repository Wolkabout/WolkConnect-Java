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

class Reading {

    private final String ref;
    private final String value;
    private final long utc;

    Reading(final String ref, final String value, final long utc) {
        this.ref = ref;
        this.value = value;
        this.utc = utc;
    }

    String getReference() {
        return ref;
    }

    String getValue() {
        return value;
    }

    long getUtc() {
        return utc;
    }

    @Override
    public String toString() {
        return "Reading {" +
                "ref='" + ref + '\'' +
                ", value='" + value + '\'' +
                ", utc=" + utc +
                '}';
    }
}
