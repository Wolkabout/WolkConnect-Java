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
package com.wolkabout.wolk.model;

public class FeedTemplate {

    private final String name;
    private final FeedType type;
    private final String unitGuid;
    private final String reference;

    public FeedTemplate(String name, FeedType type, String unitGuid, String reference) {
        this.name = name;
        this.type = type;
        this.unitGuid = unitGuid;
        this.reference = reference;
    }

    public FeedTemplate(String name, FeedType type, Unit unitGuid, String reference) {
        this(name, type, unitGuid.name(), reference);
    }

    public String getName() {
        return name;
    }

    public FeedType getType() {
        return type;
    }

    public String getUnitGuid() {
        return unitGuid;
    }

    public String getReference() {
        return reference;
    }
}
