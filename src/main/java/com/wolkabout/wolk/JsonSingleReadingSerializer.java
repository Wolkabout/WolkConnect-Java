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

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

class JsonSingleReadingSerializer extends AbstractReadingSerializer {
    private class ReadingJsonSerializer implements JsonSerializer<Reading> {
        @Override
        public JsonElement serialize(Reading reading, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("utc", reading.getUtc());
            jsonObject.addProperty("data", reading.getValue());
            return jsonObject;
        }
    }

    private final Gson gson;

    public JsonSingleReadingSerializer(String deviceKey) {
        super(deviceKey);

        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Reading.class, new ReadingJsonSerializer());
        gson = builder.create();
    }

    @Override
    public OutboundMessage serialize(List<Reading> readings) {
        if (readings.isEmpty()) {
            throw new IllegalArgumentException("Empty readings list.");
        }

        final String payload = gson.toJson(readings.get(0));
        final String topic = "readings/" + deviceKey + "/" + readings.get(0).getReference();
        return new OutboundMessage(payload, topic, 1);
    }
}
