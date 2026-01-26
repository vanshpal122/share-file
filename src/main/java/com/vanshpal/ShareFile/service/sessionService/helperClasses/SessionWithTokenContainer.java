package com.vanshpal.ShareFile.service.sessionService.helperClasses;

public record SessionWithTokenContainer(
        SessionWithToken senderSession,
        SessionWithToken receiverSession
) {
}
