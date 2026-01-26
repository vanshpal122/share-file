package com.vanshpal.ShareFile.service.entityClasses;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "file_chunks")
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int chunkIndex;

    private boolean uploaded;
    private boolean downloaded;

    private String hash;

    // Many chunks belong to ONE file
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;
}
