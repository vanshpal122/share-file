package com.vanshpal.ShareFile.service;


import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    private final LocalFileStorageService localFileStorageService;


    public FileService(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    public String storeFileChunk(FileChunk chunk) {
        return localFileStorageService.storeChunk(chunk);
    }

    public String storeFile(StoredFile file) {
        return localFileStorageService.mergeFileChunks(file);
    }
}
