package com.vanshpal.ShareFile.service.HelperClasses;

import java.util.List;


public record SenderMessage(
        String senderName,
        List<FileMetadata> listOfFiles
) {
}
