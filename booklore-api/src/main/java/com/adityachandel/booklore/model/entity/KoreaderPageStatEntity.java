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
@Table(name = "koreader_page_stats")
public class KoreaderPageStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "koreader_statistics_id", nullable = false)
    private KoreaderStatisticsEntity koreaderStatistics;

    @Column(name = "page", nullable = false)
    private Integer page;

    @Column(name = "start_time", nullable = false)
    private Integer startTime;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "total_pages_at_time", nullable = false)
    private Integer totalPagesAtTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
