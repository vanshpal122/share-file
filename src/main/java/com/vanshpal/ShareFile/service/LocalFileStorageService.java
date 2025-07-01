package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
public class LocalFileStorageService {

    private final Path mainPath;
    private final Path tempPath;

    @Autowired
    public LocalFileStorageService(StorageProperties storageProperties) {
        if (storageProperties.getTempLocation().trim().isEmpty() || storageProperties.getFinalLocation().trim().isEmpty()) {
            throw new StorageException("Location cannot be empty");
        }
        this.tempPath = Paths.get(storageProperties.getTempLocation());
        this.mainPath = Paths.get(storageProperties.getFinalLocation());
    }

    public String storeChunk(FileChunk chunk) throws StorageException {
        if (chunk.file().isEmpty()) {
            throw new StorageException("File is empty");
        }
        Path deviceFileDirectory = tempPath.resolve(
                chunk.deviceID() + File.separator +
                        chunk.fileID()
        );
        try {
            Files.createDirectories(deviceFileDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create directory");
        }
        Path newFile = deviceFileDirectory.resolve(Paths.get(chunk.chunkIndex() + "")).normalize().toAbsolutePath();
        if (!newFile.startsWith(this.tempPath.normalize().toAbsolutePath())) {
            throw new StorageException("Chunk Files must be within the temp directory");
        }

        try (InputStream inputStream = chunk.file().getInputStream()) {
            Files.copy(inputStream, newFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Error while storing chunk: " + chunk.chunkIndex() + "in file" + chunk.fileID(), e);
        }
        return newFile.relativize(tempPath).toString();
    }

    public StoredFile mergeFileChunks(String fileName, String deviceName) throws StorageException {
        Path deviceFileDirectory = mainPath.resolve(deviceName);
        try {
            Files.createDirectories(deviceFileDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create directory for completed file");
        }

        Path newFilePath = deviceFileDirectory.resolve(Paths.get(fileName));
        if (!newFilePath.startsWith(this.mainPath.normalize().toAbsolutePath())) {
            throw new StorageException("New Files must be within the main directory");
        }

        return new StoredFile(fileName, 2L,0L, 0);
    }

}
