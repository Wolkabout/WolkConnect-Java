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
