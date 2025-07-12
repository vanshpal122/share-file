package com.vanshpal.ShareFile.service;


import com.vanshpal.ShareFile.FileShareRepository;
import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
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

    public Path storeFileChunk(FileChunk chunk) {
        return localFileStorageService.storeChunk(chunk);
    }

    public StoredFile storeFile(StoredFile file) {
        StoredFile newFile = localFileStorageService.mergeFileChunks(file);
        return fileShareRepository.save(newFile);
    }

    public StoredFile getFileInfo(Long fileId) {
        return fileShareRepository.findById(fileId).orElseThrow(() -> new StorageException("File not found"));
    }

    public Resource getFile(Long fileId) {
        Optional<StoredFile> storedFile = fileShareRepository.findById(fileId);
        return storedFile.map(file -> localFileStorageService.getFile(Paths.get(file.getFileRelativePath()))).orElse(null);
    }

    public Resource getFilePart(Long fileId, long start, long end) {
        Optional<StoredFile> storedFile = fileShareRepository.findById(fileId);
        return storedFile.map(file -> localFileStorageService.getSplitFileByRange(Paths.get(file.getFileRelativePath()), start, end, fileId)).orElse(null);
    }
}
