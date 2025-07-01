package com.vanshpal.ShareFile;


import com.vanshpal.ShareFile.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;


@RestController
@RequestMapping("/sharefile")
public class ShareFileController {
    private final FileService fileService;

    public ShareFileController(FileService fileService) {
        this.fileService = fileService;
    }



    @PostMapping("/upload")
    public ResponseEntity<?> storeFile(@RequestParam("file") MultipartFile file) {
        try {
            URI filePath = fileService.storeFile(file);
            return ResponseEntity.created(filePath).build();
        } catch (IOException e) {
            return ResponseEntity.ok(e.toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }



    @GetMapping
    public ResponseEntity<?> downloadFile() {
        return ResponseEntity.ok().build();
    }
}
