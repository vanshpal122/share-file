package com.vanshpal.ShareFile.service.HelperClasses;


public record StoredFile(
        String fileName, String deviceName, Long ID, Long deviceID, int numberOfChunks
) {
}
