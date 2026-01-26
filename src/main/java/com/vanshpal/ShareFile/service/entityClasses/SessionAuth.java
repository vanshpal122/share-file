package com.vanshpal.ShareFile.service.entityClasses;

import com.vanshpal.ShareFile.service.sessionService.helperClasses.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_auth")
@Getter
@Setter
@NoArgsConstructor
public class SessionAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean revoked = false;
}

