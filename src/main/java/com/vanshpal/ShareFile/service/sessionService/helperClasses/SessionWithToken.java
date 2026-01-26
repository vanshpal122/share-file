package com.vanshpal.ShareFile.service.sessionService.helperClasses;

import com.vanshpal.ShareFile.service.entityClasses.Session;

public record SessionWithToken(
        Session session,
        String token
)  {}
