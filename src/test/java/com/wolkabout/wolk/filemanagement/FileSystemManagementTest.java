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
package com.wolkabout.wolk.filemanagement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

public class FileSystemManagementTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemManagementTest.class);

    private final String testFolderName = "test";
    private final String testFolderPath;
    private final File testFolder;

    public FileSystemManagementTest() {
        testFolderPath = Paths.get(".").toAbsolutePath().normalize().toString() + "/" + testFolderName;
        testFolder = new File(testFolderPath);
    }

    @Before
    public void setUp() throws Exception {
        if (!testFolder.mkdir()) {
            LOG.warn("Test folder path already existed. Will be deleted afterwards anyways.");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (!testFolder.delete()) {
            LOG.warn("Test folder could not be deleted. Will remain existing.");
        }
    }

    @Test
    public void sampleTest() {
        FileSystemManagement management = new FileSystemManagement(testFolderPath);
    }
}
