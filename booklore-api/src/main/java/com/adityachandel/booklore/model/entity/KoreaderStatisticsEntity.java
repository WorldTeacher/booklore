package com.adityachandel.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "koreader_statistics")
public class KoreaderStatisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BookLoreUserEntity user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private BookEntity book;

    @Column(name = "koreader_book_md5", nullable = false, length = 32)
    private String koreaderBookMd5;

    @Column(name = "book_title", length = 500)
    private String bookTitle;

    @Column(name = "book_authors", length = 1000)
    private String bookAuthors;

    @Column(name = "book_series", length = 500)
    private String bookSeries;

    @Column(name = "book_language", length = 50)
    private String bookLanguage;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "total_read_time")
    private Integer totalReadTime;

    @Column(name = "total_read_pages")
    private Integer totalReadPages;

    @Column(name = "notes_count")
    private Integer notesCount;

    @Column(name = "highlights_count")
    private Integer highlightsCount;

    @Column(name = "last_open_time")
    private Integer lastOpenTime;

    @Column(name = "last_synced")
    private Instant lastSynced;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
