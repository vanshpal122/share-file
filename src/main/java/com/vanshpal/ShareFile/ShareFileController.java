package com.vanshpal.ShareFile;


import com.vanshpal.ShareFile.service.FileService;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/sharefile")
public class ShareFileController {
    private final FileService fileService;

    public ShareFileController(FileService fileService) {
        this.fileService = fileService;
    }


    @PostMapping("/upload")
    public ResponseEntity<Void> storeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileID")  Long fileID,
            @RequestParam("deviceID") Long deviceId,
            @RequestParam("deviceName") String deviceName, //check if two have same name
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("isLastChunk") boolean isLastChunk,
            @RequestParam("totalNumberOfChunks") int totalNumberOfChunks,
            UriComponentsBuilder ucb
            ) {
        String chunkPath = fileService.storeFileChunk(new FileChunk(chunkIndex, fileID, deviceId, file));
        if (isLastChunk) {
            String filePath = fileService.storeFile(new StoredFile(file.getOriginalFilename(), deviceName, fileID, deviceId, totalNumberOfChunks));
            URI locationOfFile = ucb.path(filePath).buildAndExpand(fileID).toUri();
            return ResponseEntity.created(locationOfFile).build();
        } else {
            URI locationOfChunk = ucb.path(chunkPath).buildAndExpand(chunkIndex).toUri();
            return ResponseEntity.created(locationOfChunk).build();
        }
    }

    @GetMapping
    public ResponseEntity<?> downloadFile() {
        return ResponseEntity.ok().build();
    }
}
