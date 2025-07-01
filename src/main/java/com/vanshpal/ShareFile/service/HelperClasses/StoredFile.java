package com.vanshpal.ShareFile.service.HelperClasses;


public record StoredFile(
        String fileName, Long ID, Long deviceID, int numberOfChunks
) {
}
