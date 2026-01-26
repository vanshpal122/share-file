package com.vanshpal.ShareFile.service.entityClasses;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // DB primary key (not fileId)

    @Column(nullable = false)
    private String fileId;   //logical file ID

    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Long totalChunks;
    private String hash;

    // MANY files belong to ONE session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;
}
