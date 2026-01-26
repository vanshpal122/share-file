package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.config.StorageProperties;
import com.vanshpal.ShareFile.exceptions.StorageException;
import com.vanshpal.ShareFile.service.entityClasses.FileChunk;
import com.vanshpal.ShareFile.service.entityClasses.StoredFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class LocalFileStorageService {

    private final Path mainPath;
    private final Path tempPath;


    public LocalFileStorageService(StorageProperties storageProperties) {
        this.tempPath = storageProperties.getTempLocation();
        this.mainPath = storageProperties.getFinalLocation();
    }

    public FileChunk storeChunk(int chunkIndex, String fileId, InputStream fileStream) throws StorageException, IOException {
        if (fileId == null) {
            fileId = UUID.randomUUID().toString();
        } else if (!isValidFileId(fileId)) throw new StorageException("Invalid file ID");
        Path deviceFileDirectory = tempPath
                .resolve(fileId);

        if (!deviceFileDirectory.startsWith(this.tempPath.normalize().toAbsolutePath())) {
            throw new StorageException("Invalid File ID");
        }
        try {
            Files.createDirectories(deviceFileDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create directory");
        }
        Path newFile = deviceFileDirectory.resolve(Paths.get(chunkIndex + "")).normalize().toAbsolutePath();
        if (!newFile.startsWith(this.tempPath.normalize().toAbsolutePath())) {
            throw new StorageException("Chunk Files must be within the temp directory");
        }

        Files.copy(fileStream, newFile, StandardCopyOption.REPLACE_EXISTING);
        return new FileChunk(chunkIndex, fileId);
    }

    public Resource getFile(Path filePath) {
        checkFile(filePath);
        return new FileSystemResource(this.mainPath.resolve(filePath));
    }

    public Resource getSplitFileByRange(Path filePath, long startByte, long endByte) {
        checkFile(filePath);

        Path outputFilePath = this.tempPath.resolve(Paths.get(String.valueOf(UUID.randomUUID())));

        try (RandomAccessFile file = new RandomAccessFile(this.mainPath.resolve(filePath).toFile(), "r");
             FileOutputStream outputStream = new FileOutputStream(outputFilePath.toFile())) {

            // Move the pointer to the start position
            file.seek(startByte);

            byte[] buffer = new byte[1024]; // Buffer to hold data while reading
            int bytesRead;

            // Read and write the file content from startByte to endByte
            long bytesRemaining = endByte - startByte;
            while ((bytesRead = file.read(buffer)) != -1 && bytesRemaining > 0) {
                int bytesToWrite = (int) Math.min(bytesRead, bytesRemaining);
                outputStream.write(buffer, 0, bytesToWrite);
                bytesRemaining -= bytesToWrite;
            }
            return new UrlResource(outputFilePath.toUri());

        } catch (IOException e) {
            throw new StorageException("Error while writing file chunk: " + filePath + "to" + outputFilePath, e);
        }
    }


    public static boolean isValidFileId(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return false;
        }

        try {
            // Try to parse the string as a UUID, if it fails, return false
            UUID.fromString(fileId); // If this succeeds, it's a valid UUID
            return true;
        } catch (IllegalArgumentException e) {
            // Invalid UUID format, return false
            return false;
        }
    }


    private void checkFile(Path filePath) {
        if (filePath == null) {
            throw new StorageException("Invalid file path");
        }

        Path file = this.mainPath.resolve(filePath).normalize().toAbsolutePath();

        if (!file.startsWith(this.mainPath.normalize().toAbsolutePath())) {
            throw new StorageException("File Should be in the main directory");
        }
        if (Files.notExists(this.mainPath.resolve(filePath)) && !Files.isReadable(this.mainPath.resolve(filePath))) {
            throw new StorageException("File Not Found");
        }
    }


    public static String extractFileExtension(String originalFileName) {
        int extensionIndex = originalFileName.lastIndexOf('.');
        return (extensionIndex == -1) ? "" : originalFileName.substring(extensionIndex);
    }

    public static void deleteDirectory(Path dirPath) {
        if (Files.notExists(dirPath)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dirPath)) { //main folder will also get deleted(i.e. sessionID)
            files.sorted(Comparator.reverseOrder()) // delete children before parents
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new StorageException("Error while deleting file ", e);
                        }
                    });
        } catch (IOException e) {
            throw new StorageException("Error while listing files for deletion" + dirPath, e);
        }
    }
}
