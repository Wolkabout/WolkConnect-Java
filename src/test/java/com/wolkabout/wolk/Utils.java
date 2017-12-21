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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Utils {
    static byte[] joinByteArrays(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        final byte[] result = new byte[length];

        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    static byte[] calculateSha256(byte[] bytes) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes);

            return md.digest();
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    static boolean isFileValid(File file, byte[] checksum) {
        try (FileInputStream fiStream = new FileInputStream(file)) {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");

            final byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fiStream.read(byteArray)) != -1) {
                md.update(byteArray, 0, bytesCount);
            }

            return Arrays.equals(md.digest(), checksum);
        } catch (NoSuchAlgorithmException | IOException ignored) {
        }

        return false;
    }
}
