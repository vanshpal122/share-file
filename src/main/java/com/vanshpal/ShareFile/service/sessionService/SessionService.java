package com.vanshpal.ShareFile.service.sessionService;

import com.vanshpal.ShareFile.service.sessionService.helperClasses.NegotiationData;
import com.vanshpal.ShareFile.service.entityClasses.Session;
import com.vanshpal.ShareFile.service.entityClasses.SessionAuth;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.Role;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.SessionWithToken;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.SessionWithTokenContainer;
import com.vanshpal.ShareFile.service.sessionService.repositories.SessionAuthRepository;
import com.vanshpal.ShareFile.service.sessionService.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionAuthRepository authRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionWithTokenContainer createSession(String senderId, NegotiationData ack) {

        if (ack.listOfFiles().isEmpty()) {
            throw new IllegalArgumentException("Cannot create session with 0 files");
        }

        // 1. Generate sessionId
        String sessionId = UUID.randomUUID().toString();

        // 2. Save session to DB
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setSenderId(senderId);
        session.setReceiverId(ack.user().userId());
        session.setFiles(ack.listOfFiles());
        sessionRepository.save(session);

        // 3. Generate tokens
        String senderToken = generateToken();
        String receiverToken = generateToken();

        //4. Set token expiry
        Duration tokeLifetime = Duration.ofHours(24);
        LocalDateTime expiresAt = LocalDateTime.now().plus(tokeLifetime);

        // 5. Store tokens in DB (hashed)
        SessionAuth auth = new SessionAuth();
        auth.setSession(session);
        auth.setRole(Role.SENDER);
        auth.setTokenHash(hash(senderToken));
        auth.setExpiresAt(expiresAt);

        authRepository.save(auth);

        // 6. Return info to controller
        return new SessionWithTokenContainer(new SessionWithToken(session, senderToken), new SessionWithToken(session, receiverToken));
    }

    private String generateToken() {
        byte[] bytes = new byte[32]; // 256-bit
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        // SHA-256 hash
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
