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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemManagementTest {

    // The logger
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemManagementTest.class);

    // The constant values
    private static final String SEPARATOR = "/";
    private static final String testFolderName = "test";
    private static final String testFileName = "test-file";
    // Created at creation
    private final String testFolderPath;
    private final File testFolder;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    File fileMock;
    @Mock
    File folderMock;
    private FileSystemManagement management;

    public FileSystemManagementTest() {
        testFolderPath = Paths.get(".").toAbsolutePath().normalize().toString() + SEPARATOR + testFolderName;
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
        if (Objects.requireNonNull(testFolder.listFiles()).length > 0) {
            for (File file : Objects.requireNonNull(testFolder.listFiles())) {
                if (!file.delete()) {
                    LOG.warn("File in test folder could not be deleted.");
                }
            }
        }

        if (!testFolder.delete()) {
            LOG.warn("Test folder could not be deleted. Will remain existing.");
        }
    }

    @Test
    public void checkInvalidPath() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Path given as argument is not a valid directory path.");
        new FileSystemManagement("asdf.asdf");
    }

    @Test
    public void simpleConstructorTestWithValidPath() {
        // Create the management with valid test folder path
        management = new FileSystemManagement(testFolderPath);
    }

    @Test
    public void listNoFilesAndCreateOne() throws IOException {
        // Create the management with valid test folder path
        management = new FileSystemManagement(testFolderPath);

        // Receive the file list
        List<String> files = management.listAllFiles();
        assertEquals(files.size(), 0);

        // Touch a file
        File newFile = new File(testFolderPath + SEPARATOR + testFileName);
        assertTrue(newFile.createNewFile());

        // Fetch the list again
        files = management.listAllFiles();
        assertEquals(files.size(), 1);
    }

    @Test
    public void purgeSingleFile() throws IOException {
        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Touch a file
        File newFile = new File(testFolderPath + SEPARATOR + testFileName);
        assertTrue(newFile.createNewFile());

        // Fetch the list again
        List<String> files = management.listAllFiles();
        assertEquals(files.size(), 1);

        // Purge the directory
        assertTrue(management.purgeDirectory());
    }

    @Test
    public void addMockFile() {
        // Setup the addition to be successful
        doReturn(true).when(fileMock).renameTo(any());

        // Create the manager
        management = new FileSystemManagement(testFolderPath);

        // Add the file
        assertTrue(management.addFile(fileMock));
    }

    @Test
    public void addMockNonPurgingFile() throws NoSuchFieldException, IllegalAccessException {
        // Setup the addition to be successful
        doReturn(true).when(fileMock).renameTo(any());
        doReturn(false).when(fileMock).delete();
        doReturn(new File[] {fileMock}).when(folderMock).listFiles();

        // Create the file to be found
        assertTrue(fileMock.renameTo(new File(testFolder + SEPARATOR + testFileName)));

        // Create the manager
        management = new FileSystemManagement(testFolderPath);

        // Inject a stub root folder
        Field folderFile = FileSystemManagement.class.getDeclaredField("folder");
        folderFile.setAccessible(true);
        folderFile.set(management, folderMock);

        // Attempt to purge an undelete-able file
        assertFalse(management.purgeDirectory());
    }
}
