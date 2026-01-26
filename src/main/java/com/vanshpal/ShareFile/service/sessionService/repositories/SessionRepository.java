package com.vanshpal.ShareFile.service.sessionService.repositories;

import com.vanshpal.ShareFile.service.entityClasses.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}