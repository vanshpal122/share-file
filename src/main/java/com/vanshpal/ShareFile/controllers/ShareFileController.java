package com.vanshpal.ShareFile.controllers;


import com.vanshpal.ShareFile.exceptions.StorageException;
import com.vanshpal.ShareFile.service.entityClasses.FileChunk;
import com.vanshpal.ShareFile.service.entityClasses.StoredFile;
import com.vanshpal.ShareFile.service.storageService.FileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/shareFile")
public class ShareFileController {
    private final FileService fileService;

    public ShareFileController(FileService fileService) {
        this.fileService = fileService;
    }


    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> storeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam(value = "fileId", required = false) String fileID,
            @RequestParam("isLastChunk") boolean isLastChunk,
            @RequestParam("totalNumberOfChunks") int totalNumberOfChunks,
            UriComponentsBuilder ucb,
            HttpServletRequest request
    ) {
        FileChunk fileChunk = fileService.storeFileChunk(chunkIndex, fileID, file);
        if (isLastChunk) {
            StoredFile storedFile = fileService
                    .storeFile(file.getOriginalFilename(), fileChunk.getFile().getFileId(), totalNumberOfChunks);
            String downloadURL = ServletUriComponentsBuilder
                    .fromRequestUri(request)
                    .replacePath("shareFile/download/" + storedFile.getID())
                    .toUriString();
            HashMap<String, String> response = new HashMap<>();
            response.put("fileName", storedFile.getOriginalFileName());
            response.put("downloadURL", downloadURL);
            response.put("fileSize", file.getSize() + "");
            response.put("mimeType", file.getContentType());
            return ResponseEntity.created(URI.create(downloadURL)).body(response);
        } else {
            URI locationOfChunk = ucb.path(fileChunk.getFile().getFileId()).buildAndExpand(chunkIndex).toUri();
            return ResponseEntity.created(locationOfChunk).build();
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Server is online");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<String> handleStorageException(StorageException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return new ResponseEntity<>("Invalid JSON or data error: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
