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

package com.wolkabout.wolk.util;

import java.io.*;

public class Utils {

    // StackOverflow magic.
    public static byte[] toByteArray(File file) throws IOException {
        // Open file
        try (final RandomAccessFile f = new RandomAccessFile(file, "r")) {
            // Get and check length
            final long longLength = f.length();
            final int length = (int) longLength;
            if (length != longLength) {
                throw new IOException("File size >= 2 GB");
            }
            // Read file and return data
            final byte[] data = new byte[length];
            f.readFully(data);
            return data;
        }
    }

    public static void moveFile(File src, File dest) throws IOException {
        final boolean isMoved = src.renameTo(dest);
        if (!isMoved) {
            throw new IOException("Failed to move file");
        }
    }

    public static void writeToFile(byte[] input, File output) throws IOException {
        try (final FileOutputStream fileOutputStream = new FileOutputStream(output)) {
            fileOutputStream.write(input);
        } catch (Exception e) {
            throw new IOException("Failed to write to file.");
        }
    }

}
