package com.vanshpal.ShareFile.service;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@Service
public class FileService {

    private final LocalFileStorageService localFileStorageService;


    public FileService(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    public URI storeFile(MultipartFile file) throws IOException {
        URI storedPath;
        storedPath = localFileStorageService.storeFile(file.getInputStream(), file.getOriginalFilename());
        return storedPath;
    }
}
