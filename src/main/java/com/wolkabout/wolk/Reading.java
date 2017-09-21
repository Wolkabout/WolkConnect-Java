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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

class Reading {

    private final String ref;
    private final String value;
    private long utc;

    Reading(final ReadingType type, final String value) {
        this.ref = type.getPrefix();
        this.value = value;
        this.utc = System.currentTimeMillis() / 1000;
    }

    public long getUtc() {
        return utc;
    }

    ReadingType getType() {

        return ReadingType.fromPrefix(ref);
    }

    Reading(final String ref, final String value) {
        this.ref = ref;
        this.value = value;
        this.utc = System.currentTimeMillis() / 1000;
    }

    String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Reading{" +
                "ref=" + ref +
                ", value=" + value +
                '}';
    }

    public static class ReadingSerializer implements JsonSerializer<Reading> {

        @Override
        public JsonElement serialize(Reading reading, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("utc", reading.getUtc());
            jsonObject.addProperty("data", reading.getValue());
            return jsonObject;
        }
    }
}
