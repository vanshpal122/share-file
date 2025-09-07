package com.vanshpal.ShareFile.service.HelperClasses;

import org.springframework.web.multipart.MultipartFile;

public record FileChunk(
        int chunkIndex, String fileID
) {
}
