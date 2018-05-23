/*
 * Copyright (c) 2018 Wolkabout
 */

package com.wolkabout.wolk.firmwareupdate.model;


public class ChunkRequest {

    private String fileName;
    private int chunkSize;
    private int chunkIndex;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    @Override
    public String toString() {
        return "ChunkRequest{" +
                "fileName='" + fileName + '\'' +
                ", chunkSize=" + chunkSize +
                ", chunkIndex=" + chunkIndex +
                '}';
    }
}
