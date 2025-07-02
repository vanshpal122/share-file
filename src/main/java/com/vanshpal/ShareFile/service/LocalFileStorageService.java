package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

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
            throw new StorageException("Empty Chunk Received");
        }
        Path deviceFileDirectory = tempPath
                .resolve(chunk.deviceID() + "")
                .resolve(chunk.fileID() + "");
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

    public String mergeFileChunks(StoredFile newFile) throws StorageException {
        Path deviceFileDirectory = this.mainPath.resolve(Paths.get(newFile.deviceName())).normalize().toAbsolutePath();

        //Security Check
        if (!deviceFileDirectory.startsWith(this.mainPath.normalize().toAbsolutePath())) {
            throw new StorageException("Device Folder must be within the main directory");
        }
        //Creating new directory
        try {
            Files.createDirectories(deviceFileDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create directory for completed file");
        }

        //Creating file
        Path newFilePath = deviceFileDirectory.resolve(Paths.get(newFile.fileName())).normalize().toAbsolutePath();
        if (!newFilePath.startsWith(deviceFileDirectory)) {
            throw new StorageException("New Files must be within the corresponding device directory");
        }

        if (Files.exists(newFilePath)) {
            String fileName = newFile.fileName();
            int extensionIndex = fileName.lastIndexOf('.');
            String ex = (extensionIndex == -1) ? "" : fileName.substring(extensionIndex);
            String newFileName = (extensionIndex == -1) ? fileName: fileName.substring(0, extensionIndex);
            newFilePath = deviceFileDirectory.resolve(Paths.get(newFileName + newFile.ID() + ex)).normalize().toAbsolutePath();
        }

        //Accessing corresponding chunk directory
        Path dirPath = tempPath.resolve(newFile.deviceID() + "").resolve(newFile.ID() + "");

        File dir = dirPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new StorageException("Invalid Chunk Directory");
        }

        File[] chunkFiles = dir.listFiles((_, name) -> name.matches("\\d+"));  // Only files named as integers

        if (chunkFiles == null) {
            throw new StorageException("Failed to list chunks in directory");
        }

        int actualChunkCount = chunkFiles.length;

        if (!(actualChunkCount == newFile.numberOfChunks())) {
            throw new StorageException("Invalid Number of Chunks");
        }
        Arrays.sort(chunkFiles, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));


        //Actual Merging
        try (BufferedOutputStream mergedOut = new BufferedOutputStream(Files.newOutputStream(newFilePath))) {
            for (File chunk : chunkFiles) {
                try (BufferedInputStream chunkIn = new BufferedInputStream(new FileInputStream(chunk))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = chunkIn.read(buffer)) != -1) {
                        mergedOut.write(buffer, 0, bytesRead);
                    }

                } catch (IOException e) {
                    throw new StorageException("Error while writing chunk" + chunk.getName() + "of file" + newFile.fileName());
                }
            }
            deleteTempDirectory(this.tempPath.resolve(Paths.get(newFile.deviceID() + "")));
        } catch (IOException e) {
            throw new StorageException("Error while merging chunks: " + newFile.fileName() + newFile.ID(), e);
        }
        return newFilePath.relativize(this.mainPath).toString();
    }

    private void deleteTempDirectory(Path dirPath) {
        try (Stream<Path> files = Files.walk(dirPath)) { //main folder will also get deleted(i.e. deviceID)
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
