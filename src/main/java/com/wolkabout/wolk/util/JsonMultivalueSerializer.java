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
package com.wolkabout.wolk.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonMultivalueSerializer extends StdSerializer<List<String>> {
    private static final String MULTIVALUE_DELIMITER = ",";

    public JsonMultivalueSerializer() {
        this(null);
    }

    public JsonMultivalueSerializer(Class<List<String>> t) {
        super(t);
    }

    public static String valuesToString(List<String> values) {
        String multival = "";
        for (int i = 0; i < values.size(); ++i) {
            multival += values.get(i);
            if (i != values.size() - 1) {
                multival += MULTIVALUE_DELIMITER;
            }
        }

        return multival;
    }

    public static List<String> valuesFromString(String value) {
        return new ArrayList<String>(Arrays.asList(value.split(MULTIVALUE_DELIMITER)));
    }

    @Override
    public void serialize(List<String> values, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(valuesToString(values));
    }
}
