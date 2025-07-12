package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
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
import java.util.stream.Stream;

@Service
public class LocalFileStorageService {

    private final Path mainPath;
    private final Path tempPath;


    public LocalFileStorageService(StorageProperties storageProperties) {
        this.tempPath = storageProperties.getTempLocation();
        this.mainPath = storageProperties.getFinalLocation();
    }

    public Path storeChunk(FileChunk chunk) throws StorageException {
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
        return tempPath.relativize(newFile);
    }

    public StoredFile mergeFileChunks(StoredFile newFile) throws StorageException {
        Path deviceFileDirectory = this.mainPath.resolve(Paths.get(newFile.getDeviceName())).normalize().toAbsolutePath();

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
        Path newFilePath = deviceFileDirectory.resolve(Paths.get(newFile.getOriginalFileName())).normalize().toAbsolutePath();
        if (!newFilePath.startsWith(deviceFileDirectory)) {
            throw new StorageException("New Files must be within the corresponding device directory");
        }

        if (Files.exists(newFilePath)) {
            String changedFileName = appendToFileNameRespectingExtension(newFile.getOriginalFileName(), newFile.getID() + "");
            newFile.setStoredFileName(changedFileName);
            newFilePath = deviceFileDirectory.resolve(Paths.get(changedFileName)).normalize().toAbsolutePath();
        }

        //Accessing corresponding chunk directory
        Path dirPath = tempPath.resolve(newFile.getDeviceID() + "").resolve(newFile.getID() + "");

        File dir = dirPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new StorageException("Invalid Chunk Directory");
        }

        File[] chunkFiles = dir.listFiles((_, name) -> name.matches("\\d+"));  // Only files named as integers

        if (chunkFiles == null) {
            throw new StorageException("Failed to list chunks in directory");
        }

        int actualChunkCount = chunkFiles.length;

        if (!(actualChunkCount == newFile.getNumberOfChunks())) {
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
                    throw new StorageException("Error while writing chunk" + chunk.getName() + "of file with id" + newFile.getID(), e);
                }
            }
            deleteDirectory(this.tempPath.resolve(Paths.get(newFile.getDeviceID() + "")));
        } catch (IOException e) {
            throw new StorageException("Error while merging chunks: " + newFile.getID(), e);
        }
        newFile.setFileRelativePath(mainPath.relativize(newFilePath).toString());
        newFile.setID(null);
        try {
            newFile.setFileSize(Files.size(newFilePath));
            newFile.setFileType(Files.probeContentType(newFilePath));
        } catch (IOException e) {
            throw new StorageException("Error determining size of file");
        }
        return newFile;
    }

    public Resource getFile(Path filePath) {
        checkFile(filePath);
        return new FileSystemResource(this.mainPath.resolve(filePath));
    }

    public Resource getSplitFileByRange(Path filePath, long startByte, long endByte, long fileId) {
        checkFile(filePath);

        Path outputFilePath = this.tempPath.resolve(Paths.get(String.valueOf(fileId)));

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

    private void checkFile(Path filePath) {
        if(filePath == null) {
            throw new StorageException("Invalid file path");
        }

        Path file = this.mainPath.resolve(filePath).normalize().toAbsolutePath();

        if (!file.startsWith(this.mainPath.normalize().toAbsolutePath())) {
            throw new StorageException("File Should be in the main directory");
        }
        if(Files.notExists(this.mainPath.resolve(filePath)) && !Files.isReadable(this.mainPath.resolve(filePath))) {
            throw new StorageException("File Not Found");
        }
    }


    public static String appendToFileNameRespectingExtension(String originalFileName, String textToAppend) {
        int extensionIndex = originalFileName.lastIndexOf('.');
        String ex = (extensionIndex == -1) ? "" : originalFileName.substring(extensionIndex);
        String fileNameWithoutExtension = (extensionIndex == -1) ? originalFileName : originalFileName.substring(0, extensionIndex);
        return (fileNameWithoutExtension + textToAppend + ex);
    }

    public static void deleteDirectory(Path dirPath) {
        if (Files.notExists(dirPath)) {
            return;
        }
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
