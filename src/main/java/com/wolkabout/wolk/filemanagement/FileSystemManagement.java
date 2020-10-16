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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is a class that contains all the information and logic about the file system,
 * to notify the outside of all files we contain, store a new file from given bytes and name,
 * delete/purge files.
 */
public final class FileSystemManagement {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemManagement.class);

    // Constants
    private static final String SEPARATOR = "/";
    // Given arguments
    private final File folder;

    /**
     * The default constructor for the class. The given path needs to be a path to a folder where this manager
     * will work. In this folder, all the files received will be stored, files will be listed, and deleted/purged.
     *
     * @param folderPath The absolute/relative path to the folder where the manager will work.
     */
    public FileSystemManagement(String folderPath) throws IOException {
        // Give it a new `File` instance and check whether the path is a valid directory path
        this.folder = new File(folderPath);
        if (!this.folder.isDirectory()) {
            if (!this.folder.mkdir()) {
                throw new IllegalArgumentException("Path given as argument is not a valid directory path.");
            }
        }
        LOG.debug("Initialized file system management for absolute path '" + this.folder.getAbsolutePath() + "'.");
    }

    /**
     * This is the method used to list all the files found in the path given to the Management instance. This will list
     * only name of all files, and no folders.
     *
     * @return List of all direct file names as an ArrayList.
     */
    public List<String> listAllFiles() throws IOException {
        // Create the list where to store all the file names
        LOG.debug("Peeking the file system for all files.");
        ArrayList<String> files = new ArrayList<>();

        try {
            // List through all the files
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
        } catch (NullPointerException exception) {
            throw new IOException("Could not read folder contents.");
        }

        // Return all the names
        LOG.debug("Found " + files.size() + " files.");
        return files;
    }

    /**
     * This is the method used to attempt to obtain a file found in the folder. If it exists, its instance as File
     * will be returned. Otherwise, a null will be returned.
     *
     * @param fileName The name for the file we are looking for.
     * @return The instance of File we are looking for.
     */
    public File getFile(String fileName) {
        // List through all the files
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }

        return null;
    }

    /**
     * This is the method used to add a new already existing folder to the directory.
     *
     * @param newFile Instanced file that can be already found on the file system.
     * @return Success status of the operation.
     */
    public boolean addFile(File newFile) {
        LOG.debug("Attempting to add an existing file '" + newFile.getName() +
                "'@'" + newFile.getAbsolutePath() + "'.");
        return newFile.renameTo(new File(folder.getAbsolutePath() + SEPARATOR + newFile.getName()));
    }

    /**
     * This is the method used to create a file from passed bytes, and a name given.
     *
     * @param bytes    The bytes that should be written to the file.
     * @param fileName The name of the new file that will be created.
     * @return Success status of the operation.
     */
    public boolean createFile(byte[] bytes, String fileName) throws IOException {
        LOG.debug("Attempting to create file '" + fileName + "' with " + bytes.length + " bytes.");
        try (FileOutputStream stream = new FileOutputStream(folder.getAbsolutePath() + SEPARATOR + fileName)) {
            stream.write(bytes);
            return true;
        } catch (IOException exception) {
            LOG.error(exception.getLocalizedMessage());
            throw exception;
        }
    }

    /**
     * This is the method used to delete a single file that can be found in the directory.
     *
     * @param fileName The exact name for the file that user wishes to delete.
     * @return Success status of the operation.
     */
    public boolean deleteFile(String fileName) {
        // Iterate through all the files
        LOG.debug("Attempting to delete file '" + fileName + "' from the file system.");
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().equals(fileName)) {
                return file.delete();
            }
        }

        // If nothing happened, just return false
        return false;
    }

    /**
     * This is the method used to purge all the directory contents, by removing all the files located in the folder.
     *
     * @return Success of this operation
     */
    public boolean purgeDirectory() {
        // Create a place to record status
        LOG.debug("Purging the file system directory.");
        boolean failures = false;

        // Iterate through all the files
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (!file.delete()) {
                LOG.warn("Failed to delete file '" + file.getName() + "' while purging directory.");
                failures = true;
            }
        }

        // Return the status
        return !failures;
    }
}
