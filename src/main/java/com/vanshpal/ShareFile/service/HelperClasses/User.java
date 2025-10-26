package com.vanshpal.ShareFile.service.HelperClasses;

public record User(String sessionId, String userName) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}

