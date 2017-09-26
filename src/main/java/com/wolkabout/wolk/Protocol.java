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

public enum Protocol {
    WolkSense("sensors/"), JsonSingle("readings/"), JsonBulk("readings/");

    private String readingsTopic;

    Protocol(String readingsTopic) {
        this.readingsTopic = readingsTopic;
    }

    public String getReadingsTopic() {
        return readingsTopic;
    }
}
