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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is a class that contains all the information and logic about the file system,
 * to notify the outside of all files we contain, store a new file from given bytes and name,
 * delete/purge files.
 */
public class FileSystemManagement {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemManagement.class);

    // Given arguments
    private final File folder;

    /**
     * The default constructor for the class. The given path needs to be a path to a folder where this manager
     * will work. In this folder, all the files received will be stored, files will be listed, and deleted/purged.
     *
     * @param folderPath The absolute/relative path to the folder where the manager will work.
     */
    public FileSystemManagement(String folderPath) {
        // Give it a new `File` instance and check whether the path is a valid directory path
        this.folder = new File(folderPath);
        if (!this.folder.isDirectory()) {
            throw new IllegalArgumentException("Path given as argument is not a valid directory path.");
        }
    }

    /**
     * This is the method used to list all the files found in the path given to the Management instance. This will list
     * only name of all files, and no folders.
     *
     * @return List of all direct file names as an ArrayList.
     */
    public List<String> listAllFiles() {
        // Create the list where to store all the file names
        ArrayList<String> files = new ArrayList<>();

        // List through all the files
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile()) {
                files.add(file.getName());
            }
        }

        // Return all the names
        return files;
    }
}
