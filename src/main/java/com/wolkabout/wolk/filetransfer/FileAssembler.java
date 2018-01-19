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
package com.wolkabout.wolk.filetransfer;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(FileAssembler.class);

    private static final long PACKET_SIZE = (1024 * 1024) + 64; // 1 MB file chunk + 64 B previous and current packet hashes

    public enum PacketProcessingError {
        NONE,
        OUT_OF_ORDER,
        INVALID_CHECKSUM,
        RETRY_COUNT_EXCEEDED,
        UNABLE_TO_FINALIZE_FILE_CREATION
    }

    private final Path downloadDirectory;
    private final int maxTriesPerPacket;

    private int currentPacketTryCount;

    private FileReceiver listener;

    private long totalPacketCount;
    private long currentPacketId;
    private byte[] previousPacketHash;

    private File constructedFile;
    private File tmpFile;
    private byte[] fileSha256;

    private FileOutputStream tmpFileOutputStream;

    public FileAssembler(Path downloadDirectory, int maxTriesPerPacket) {
        this.downloadDirectory = downloadDirectory;
        this.maxTriesPerPacket = maxTriesPerPacket;
    }

    public boolean initialize(String fileName, long fileSize, byte[] fileSha256) {
        try {
            currentPacketTryCount = 0;

            totalPacketCount = (long) Math.ceil((double) fileSize / PACKET_SIZE);
            currentPacketId = 0;
            previousPacketHash = new byte[32];
            Arrays.fill(previousPacketHash, (byte) 0);

            tmpFile = File.createTempFile(fileName, null);
            tmpFile.deleteOnExit();

            constructedFile = Paths.get(downloadDirectory.toString(), fileName).toFile();
            this.fileSha256 = fileSha256;

            tmpFileOutputStream = new FileOutputStream(tmpFile);
            return true;
        } catch (IOException e) {
            LOG.error("Unable to create temporary file", e);
            return false;
        }
    }

    public PacketProcessingError processPacket(FileTransferPacket packet) {
        LOG.debug("Processing {}", packet);

        if (!isPacketInOrder(packet)) {
            LOG.error("Discarding packet. Reason: Packet is out of order");
            if (++currentPacketTryCount >= maxTriesPerPacket) {
                abort();
                return PacketProcessingError.RETRY_COUNT_EXCEEDED;
            }

            return PacketProcessingError.OUT_OF_ORDER;
        }

        if (!packet.isChecksumValid()) {
            LOG.error("Discarding packet. Reason: Packet checksum invalid");
            currentPacketTryCount += 1;
            if (currentPacketTryCount >= maxTriesPerPacket) {
                abort();
                return PacketProcessingError.RETRY_COUNT_EXCEEDED;
            }

            return PacketProcessingError.INVALID_CHECKSUM;
        }

        appendDataChunkToFile(packet);
        currentPacketTryCount = 0;

        if (!allPacketsReceived()) {
            return PacketProcessingError.NONE;
        }

        if (!createAndValidateFile()) {
            return PacketProcessingError.UNABLE_TO_FINALIZE_FILE_CREATION;
        }

        try {
            listenerOnFileCreated(constructedFile.getCanonicalFile().toPath());
            return PacketProcessingError.NONE;
        } catch (IOException e) {
            LOG.error("Unable to convert file path to canonical one", e);
            return PacketProcessingError.UNABLE_TO_FINALIZE_FILE_CREATION;
        }
    }

    public FileTransferPacketRequest packetRequest() {
        if (allPacketsReceived()) {
            return null;
        }

        return new FileTransferPacketRequest(constructedFile.getName(), currentPacketId, PACKET_SIZE);
    }

    public void abort() {
        LOG.info("Aborting file construction");
        closeFileStream();
    }

    private boolean allPacketsReceived() {
        //  Packet ID starts from 0
        return currentPacketId >= totalPacketCount;
    }

    private boolean isPacketInOrder(FileTransferPacket packet) {
        return Arrays.equals(previousPacketHash, packet.getPreviousPacketHash());
    }

    private void appendDataChunkToFile(FileTransferPacket packet) {
        try {
            LOG.debug("Appending file chunk to temporary file");
            tmpFileOutputStream.write(packet.getData());

            currentPacketId += 1;
            previousPacketHash = packet.getHash();
        } catch (IOException e) {
            LOG.error("Unable to append file chunk to temporary file", e);
        }
    }

    private boolean createAndValidateFile() {
        LOG.info("Creating and validating file {}", constructedFile.toString());

        if (constructedFile.exists() && !constructedFile.delete()) {
            LOG.error("Unable to remove stale firmware file (Stale firmware file and new firmware file have same name)");
            return false;
        }

        LOG.debug("Moving temporary file {} to {}", tmpFile, constructedFile.toPath().toAbsolutePath());
        if (!closeFileStream() || !moveFile(tmpFile.toPath(), constructedFile.toPath())) {
            LOG.error("Unable to move temporary file");
            return false;
        }

        LOG.debug("Verifying file integrity via SHA-256 checksum");
        if (!isFileSha256Valid(constructedFile, fileSha256)) {
            LOG.error("File integrity violated");
            return false;
        }

        return true;
    }

    private boolean closeFileStream() {
        try {
            LOG.trace("Closing file stream");
            tmpFileOutputStream.close();
            return true;
        } catch (IOException e) {
            LOG.error("Unable to close file stream", e);
            return false;
        }
    }

    private boolean moveFile(Path source, Path destination) {
        try {
            LOG.trace("Moving file {} -> {}", source.toAbsolutePath(), destination.toAbsolutePath());
            Files.move(source, destination);
            return true;
        } catch (IOException e) {
            LOG.error("Unable to move file", e);
            return false;
        }
    }

    private boolean isFileSha256Valid(File file, byte[] checksum) {
        try {
            final byte[] fileBytes = DigestUtils.sha256(Files.readAllBytes(file.toPath()));
            return Arrays.equals(checksum, fileBytes);
        } catch (IOException e) {
            LOG.error("Unable to verify file SHA-256", e);
            return false;
        }
    }

    public void setListener(FileReceiver listener) {
        this.listener = listener;
    }


    private void listenerOnFileCreated(Path file) {
        if (listener != null) {
            listener.onFileReceived(file);
        }
    }
}
