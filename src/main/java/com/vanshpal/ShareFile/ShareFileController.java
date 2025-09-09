package com.vanshpal.ShareFile;


import com.vanshpal.ShareFile.service.Exceptions.StorageException;
import com.vanshpal.ShareFile.service.FileService;
import com.vanshpal.ShareFile.service.HelperClasses.ByteRange;
import com.vanshpal.ShareFile.service.HelperClasses.FileChunk;
import com.vanshpal.ShareFile.service.HelperClasses.StoredFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

    private ByteRange checkRangeSatisfiable(String range, long fileLength) {
        if (range == null) {
            return null;
        }
        int idx = range.indexOf('-');
        if (idx == 0) {
            long offset = Long.parseLong(range.substring(idx + 1));
            long start = (offset < fileLength) ? (fileLength - offset) : 0;
            return new ByteRange(start, fileLength - 1);
        } else if (idx == range.length() - 1) {
            long start = Long.parseLong(range.substring(0, idx));
            if (start >= fileLength) return null;
            return new ByteRange(start, fileLength - 1);
        } else {
            long start = Long.parseLong(range.substring(0, idx));
            if (start >= fileLength) return null;
            long end = Long.parseLong(range.substring(idx + 1));
            if (end >= fileLength) end = fileLength - 1;
            return new ByteRange(start, end);
        }
    }

    private boolean checkRangeValidity(String range) {
        int idx = range.indexOf('-');
        if (idx == -1) return false;
        if (idx != range.lastIndexOf('-')) {
            return false;
        }

        //suffix-range
        long suffixOffset = Long.parseLong(range.substring(idx + 1));
        if (idx == 0) {
            try {
                return suffixOffset > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (idx == range.length() - 1) {
            try {
                long start = Long.parseLong(range.substring(0, idx));
                return start > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            //int-range
            try {
                long start = Long.parseLong(range.substring(0, idx));
                return (start >= 0 && suffixOffset >= 0 && start <= suffixOffset);
            } catch (NumberFormatException e) {
                return false;
            }
        }
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
                    .storeFile(file.getOriginalFilename(), fileChunk.fileID(), totalNumberOfChunks);
            String downloadURL = ServletUriComponentsBuilder
                    .fromRequestUri(request)
                    .replacePath("shareFile/download/" + storedFile.getID())
                    .toUriString();
            HashMap<String, String> response = new HashMap<>();
            response.put("fileName", storedFile.getOriginalFileName());
            response.put("downloadURL", downloadURL);
            response.put("fileSize", file.getSize() + "");
            response.put("fileType", file.getContentType());
            return ResponseEntity.created(URI.create(downloadURL)).body(response);
        } else {
            URI locationOfChunk = ucb.path(fileChunk.fileID()).buildAndExpand(chunkIndex).toUri();
            return ResponseEntity.created(locationOfChunk).build();
        }
    }


    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        if (rangeHeader != null && !rangeHeader.startsWith("bytes="))
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();

        StoredFile file = fileService.getFileInfo(fileId);

        long rangeStart = 0;
        long rangeEnd = file.getFileSize() - 1;
        boolean isRange = (rangeHeader != null);
        ByteRange consideredRange = null;

        if (isRange) {
            String[] ranges = rangeHeader.substring(6).split(",");
            for (String range : ranges) {
                if (!checkRangeValidity(range)) {
                    isRange = false;
                    break;
                } else {
                    if (consideredRange == null) consideredRange = checkRangeSatisfiable(range, file.getFileSize());
                }
            }
        }
        if (isRange) {
            if (consideredRange == null) {
                return ResponseEntity
                        .status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes" + " " + "*/" + file.getFileSize())
                        .build();
            } else {
                rangeStart = consideredRange.startByte();
                rangeEnd = consideredRange.endByte();
            }
        }
        long contentLength = rangeEnd - rangeStart + 1;
        Resource resource = (!isRange || (contentLength == file.getFileSize())) ? fileService.getFile(fileId) : fileService.getFilePart(fileId, rangeStart, rangeEnd);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(isRange ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getFileType())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + file.getFileSize())
                .body(resource);
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
