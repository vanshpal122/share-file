package com.vanshpal.ShareFile.service.sessionService.helperClasses;

import com.vanshpal.ShareFile.service.entityClasses.FileMetadata;
import com.vanshpal.ShareFile.service.entityClasses.User;

import java.util.List;


public record NegotiationData(
        User user,
        String negotiationId,
        List<FileMetadata> listOfFiles
) {
}
