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

import com.wolkabout.wolk.filemanagement.model.device2platform.FileInformation;
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
import static org.mockito.Mockito.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
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
    private final byte[] testBytes;
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

        testBytes = new byte[]{0, 123, 55, 125};
    }

    @Before
    public void setUp() {
        if (!testFolder.mkdir()) {
            LOG.warn("Test folder path already existed. Will be deleted afterwards anyways.");
        }
    }

    @After
    public void tearDown() {
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
    public void checkInvalidPath() throws IOException {
        String testPath = "test/asdfasdf";
        File existingFolder = new File(testPath);
        existingFolder.createNewFile();
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Path given as argument is not a valid directory path.");
        new FileSystemManagement(testPath);
    }

    @Test
    public void simpleConstructorTestWithValidPath() throws IOException {
        // Create the management with valid test folder path
        management = new FileSystemManagement(testFolderPath);
    }

    @Test
    public void listNoFilesAndCreateOne() throws IOException {
        // Create the management with valid test folder path
        management = new FileSystemManagement(testFolderPath);

        // Receive the file list
        List<FileInformation> files = management.listAllFiles();
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
        List<FileInformation> files = management.listAllFiles();
        assertEquals(files.size(), 1);

        // Purge the directory
        assertTrue(management.purgeDirectory());
    }

    @Test
    public void addMockFile() throws IOException {
        // Setup the addition to be successful
        doReturn(true).when(fileMock).renameTo(any());

        // Create the manager
        management = new FileSystemManagement(testFolderPath);

        // Add the file
        assertTrue(management.addFile(fileMock));

        // Verify the mock was used
        verify(fileMock, times(1)).renameTo(any());
    }

    @Test
    public void addMockNonPurgingFile() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Setup the addition to be successful
        doReturn(true).when(fileMock).renameTo(any());
        doReturn(false).when(fileMock).delete();
        doReturn(new File[]{fileMock}).when(folderMock).listFiles();

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

        // Verify everything was called
        verify(fileMock, times(1)).renameTo(any());
        verify(fileMock, times(1)).delete();
        verify(folderMock, times(1)).listFiles();
    }

    @Test
    public void createFileFromBytes() throws IOException {
        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Create the file
        assertTrue(management.createFile(testBytes, testFileName));
    }

    @Test
    public void createFileThatCannotBeCreated() throws IOException {
        // Create the hypothetical file
        String invalidFilePath = "this/path/totally/does/not/exist";

        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Create the file
        exceptionRule.expect(IOException.class);
        management.createFile(testBytes, invalidFilePath);
    }

    @Test
    public void deleteExistingFile() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Setup the folder mock
        doReturn(testFileName).when(fileMock).getName();
        doReturn(true).when(fileMock).delete();
        doReturn(new File[]{fileMock}).when(folderMock).listFiles();

        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Inject the mock
        Field folderFile = FileSystemManagement.class.getDeclaredField("folder");
        folderFile.setAccessible(true);
        folderFile.set(management, folderMock);

        // Delete the mock file
        assertTrue(management.deleteFile(testFileName));

        // Verify the mocks were called
        verify(fileMock, times(1)).getName();
        verify(fileMock, times(1)).delete();
        verify(folderMock, times(1)).listFiles();
    }

    @Test
    public void deleteFileThatDoesNotDelete() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Setup the folder mock
        doReturn(testFileName).when(fileMock).getName();
        doReturn(false).when(fileMock).delete();
        doReturn(new File[]{fileMock}).when(folderMock).listFiles();

        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Inject the mock
        Field folderFile = FileSystemManagement.class.getDeclaredField("folder");
        folderFile.setAccessible(true);
        folderFile.set(management, folderMock);

        // Delete the mock file
        assertFalse(management.deleteFile(testFileName));

        // Verify the mocks were called
        verify(fileMock, times(1)).getName();
        verify(fileMock, times(1)).delete();
        verify(folderMock, times(1)).listFiles();
    }

    @Test
    public void deleteFileThatDoesNotExist() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Setup the folder mock
        doReturn(testFileName).when(fileMock).getName();
        doReturn(new File[]{fileMock}).when(folderMock).listFiles();

        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Inject the mock
        Field folderFile = FileSystemManagement.class.getDeclaredField("folder");
        folderFile.setAccessible(true);
        folderFile.set(management, folderMock);

        // Delete the mock file
        assertFalse(management.deleteFile("asdf.asdf"));

        // Verify the mocks were called
        verify(fileMock, times(1)).getName();
        verify(fileMock, times(0)).delete();
        verify(folderMock, times(1)).listFiles();
    }

    @Test
    public void testGetFileHappyFlow() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Setup the folder mock
        doReturn(testFileName).when(fileMock).getName();
        doReturn(new File[]{fileMock}).when(folderMock).listFiles();

        // Create the management
        management = new FileSystemManagement(testFolderPath);

        // Inject the mock
        Field folderFile = FileSystemManagement.class.getDeclaredField("folder");
        folderFile.setAccessible(true);
        folderFile.set(management, folderMock);

        // Get the file
        assertEquals(fileMock, management.getFile(testFileName));
        assertNull(management.getFile("asdf"));

        // Verify the mocks were called
        verify(fileMock, times(2)).getName();
        verify(folderMock, times(2)).listFiles();
    }
}
