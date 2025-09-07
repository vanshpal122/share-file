package com.vanshpal.ShareFile.service.HelperClasses;

public record FileMetadata (
        String fileName,
        Long fileSize,
        String fileType,
        String downloadURL
) {}
