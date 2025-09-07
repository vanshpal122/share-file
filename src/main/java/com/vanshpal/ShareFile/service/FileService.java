package com.vanshpal.ShareFile.service;


import com.vanshpal.ShareFile.FileShareRepository;
import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class FileService {

    private final LocalFileStorageService localFileStorageService;
    private final FileShareRepository fileShareRepository;


    public FileService(LocalFileStorageService localFileStorageService, FileShareRepository fileShareRepository) {
        this.localFileStorageService = localFileStorageService;
        this.fileShareRepository = fileShareRepository;
    }

    public FileChunk storeFileChunk(int chunkIndex, String fileId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Empty Chunk Received");
        }
        FileChunk fileChunk;
        try (InputStream inputStream = file.getInputStream()) {
            fileChunk = localFileStorageService.storeChunk(chunkIndex, fileId, inputStream);
        } catch (IOException e) {
            throw new StorageException("Error while storing chunk: " + chunkIndex + "in file" + fileId, e);
        }
        return fileChunk;
    }

    public StoredFile storeFile(String fileName, String fileId, int totalNumberOfChunks) {
        StoredFile newFile = localFileStorageService.mergeFileChunks(fileName, fileId, totalNumberOfChunks);
        return fileShareRepository.save(newFile);
    }

    public StoredFile getFileInfo(Long fileId) {
        return fileShareRepository.findById(fileId).orElseThrow(() -> new StorageException("File not found"));
    }

    public Resource getFile(Long fileId) {
        Optional<StoredFile> storedFile = fileShareRepository.findById(fileId);
        return storedFile.map(file -> localFileStorageService.getFile(Paths.get(file.getStoredFileName()))).orElse(null);
    }

    public Resource getFilePart(Long fileId, long start, long end) {
        Optional<StoredFile> storedFile = fileShareRepository.findById(fileId);
        return storedFile.map(file -> localFileStorageService.getSplitFileByRange(Paths.get(file.getStoredFileName()), start, end)).orElse(null);
    }
}
