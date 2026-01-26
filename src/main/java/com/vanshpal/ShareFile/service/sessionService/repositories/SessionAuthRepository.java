package com.vanshpal.ShareFile.service.sessionService.repositories;

import com.vanshpal.ShareFile.service.entityClasses.SessionAuth;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionAuthRepository extends JpaRepository<SessionAuth, Long> {
    Optional<SessionAuth> findBySessionIdAndRole(String sessionId, Role role);

    Optional<SessionAuth> findByTokenHash(String tokenHash);

    List<SessionAuth> findByExpiresAtBefore(LocalDateTime time);
}
