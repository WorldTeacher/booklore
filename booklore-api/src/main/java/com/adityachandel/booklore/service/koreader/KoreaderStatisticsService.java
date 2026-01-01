package com.adityachandel.booklore.service.koreader;

import com.adityachandel.booklore.config.security.service.AuthenticationService;
import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.dto.response.KoreaderStatisticsResponse;
import com.adityachandel.booklore.model.entity.*;
import com.adityachandel.booklore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KoreaderStatisticsService {

    private final AuthenticationService authenticationService;
    private final KoreaderStatisticsRepository koreaderStatisticsRepository;
    private final KoreaderPageStatRepository koreaderPageStatRepository;
    private final BookRepository bookRepository;
    private final UserBookProgressRepository userBookProgressRepository;

    @Transactional
    public void processStatisticsDatabase(File sqliteFile, BookLoreUserEntity user) {
        log.info("Processing KOReader statistics database for user: {}", user.getId());

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath())) {
            List<KoreaderBookStats> bookStats = readBookStatistics(conn);
            
            for (KoreaderBookStats stats : bookStats) {
                processBookStatistics(stats, conn, user);
            }
            
            log.info("Successfully processed {} books from statistics database", bookStats.size());
        } catch (SQLException e) {
            log.error("Failed to process KOReader statistics database", e);
            throw new RuntimeException("Failed to process statistics database", e);
        }
    }

    private List<KoreaderBookStats> readBookStatistics(Connection conn) throws SQLException {
        List<KoreaderBookStats> bookStats = new ArrayList<>();
        
        String query = "SELECT title, authors, notes, last_open, highlights, pages, series, language, md5, " +
                      "total_read_time, total_read_pages FROM book";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                KoreaderBookStats stats = new KoreaderBookStats();
                stats.title = rs.getString("title");
                stats.authors = rs.getString("authors");
                stats.notes = getIntOrNull(rs, "notes");
                stats.lastOpen = getIntOrNull(rs, "last_open");
                stats.highlights = getIntOrNull(rs, "highlights");
                stats.pages = getIntOrNull(rs, "pages");
                stats.series = rs.getString("series");
                stats.language = rs.getString("language");
                stats.md5 = rs.getString("md5");
                stats.totalReadTime = getIntOrNull(rs, "total_read_time");
                stats.totalReadPages = getIntOrNull(rs, "total_read_pages");
                
                bookStats.add(stats);
            }
        }
        
        return bookStats;
    }

    @Transactional
    public void processBookStatistics(KoreaderBookStats stats, Connection conn, BookLoreUserEntity user) {
        if (stats.md5 == null || stats.md5.isEmpty()) {
            log.warn("Skipping book with no MD5 hash: {}", stats.title);
            return;
        }

        // Find or create statistics entity
        KoreaderStatisticsEntity statisticsEntity = koreaderStatisticsRepository
                .findByUserIdAndKoreaderBookMd5(user.getId(), stats.md5)
                .orElseGet(() -> {
                    KoreaderStatisticsEntity newEntity = new KoreaderStatisticsEntity();
                    newEntity.setUser(user);
                    newEntity.setKoreaderBookMd5(stats.md5);
                    return newEntity;
                });

        // Try to match with existing book
        Optional<BookEntity> matchedBook = bookRepository.findByCurrentHash(stats.md5);
        matchedBook.ifPresent(statisticsEntity::setBook);

        // Update statistics
        statisticsEntity.setBookTitle(stats.title);
        statisticsEntity.setBookAuthors(stats.authors);
        statisticsEntity.setBookSeries(stats.series);
        statisticsEntity.setBookLanguage(stats.language);
        statisticsEntity.setTotalPages(stats.pages);
        statisticsEntity.setTotalReadTime(stats.totalReadTime);
        statisticsEntity.setTotalReadPages(stats.totalReadPages);
        statisticsEntity.setNotesCount(stats.notes);
        statisticsEntity.setHighlightsCount(stats.highlights);
        statisticsEntity.setLastOpenTime(stats.lastOpen);
        statisticsEntity.setLastSynced(Instant.now());

        statisticsEntity = koreaderStatisticsRepository.save(statisticsEntity);

        // Read and save page statistics
        savePageStatistics(conn, stats.md5, statisticsEntity);

        // Update user progress if book is matched
        if (matchedBook.isPresent()) {
            updateUserProgress(user, matchedBook.get(), stats);
        }
    }

    private void savePageStatistics(Connection conn, String bookMd5, KoreaderStatisticsEntity statisticsEntity) {
        try {
            // Delete old page stats
            koreaderPageStatRepository.deleteByKoreaderStatisticsId(statisticsEntity.getId());

            String query = "SELECT page, start_time, duration, total_pages FROM page_stat_data " +
                          "WHERE id_book = (SELECT id FROM book WHERE md5 = ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, bookMd5);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<KoreaderPageStatEntity> pageStats = new ArrayList<>();
                    
                    while (rs.next()) {
                        KoreaderPageStatEntity pageStat = KoreaderPageStatEntity.builder()
                                .koreaderStatistics(statisticsEntity)
                                .page(rs.getInt("page"))
                                .startTime(rs.getInt("start_time"))
                                .duration(rs.getInt("duration"))
                                .totalPagesAtTime(rs.getInt("total_pages"))
                                .build();
                        
                        pageStats.add(pageStat);
                    }
                    
                    if (!pageStats.isEmpty()) {
                        koreaderPageStatRepository.saveAll(pageStats);
                        log.debug("Saved {} page statistics for book: {}", pageStats.size(), statisticsEntity.getBookTitle());
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to save page statistics for book: {}", statisticsEntity.getBookTitle(), e);
        }
    }

    private void updateUserProgress(BookLoreUserEntity user, BookEntity book, KoreaderBookStats stats) {
        Optional<UserBookProgressEntity> existingProgress = 
                userBookProgressRepository.findByUserIdAndBookId(user.getId(), book.getId());

        UserBookProgressEntity progress = existingProgress.orElseGet(() -> {
            UserBookProgressEntity newProgress = new UserBookProgressEntity();
            newProgress.setUser(user);
            newProgress.setBook(book);
            return newProgress;
        });

        // Update the progress with statistics data
        // Note: We don't override existing KOReader sync progress, just add statistics
        progress.setLastReadTime(Instant.now());

        userBookProgressRepository.save(progress);
        log.info("Updated progress for book: {} with statistics data", book.getMetadata().getTitle());
    }

    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    @Transactional(readOnly = true)
    public List<KoreaderStatisticsResponse> getUserStatistics() {
        BookLoreUser authenticatedUser = authenticationService.getAuthenticatedUser();
        Long userId = authenticatedUser.getId();

        List<KoreaderStatisticsEntity> statistics = koreaderStatisticsRepository.findByUserId(userId);
        
        return statistics.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KoreaderStatisticsResponse getBookStatistics(Long bookId) {
        BookLoreUser authenticatedUser = authenticationService.getAuthenticatedUser();
        Long userId = authenticatedUser.getId();

        List<KoreaderStatisticsEntity> statistics = koreaderStatisticsRepository
                .findByUserIdAndBookId(userId, bookId);

        if (statistics.isEmpty()) {
            throw ApiError.GENERIC_NOT_FOUND.createException("No KOReader statistics found for this book");
        }

        // Return the most recently synced statistics
        KoreaderStatisticsEntity mostRecent = statistics.stream()
                .max((a, b) -> {
                    Instant timeA = a.getLastSynced() != null ? a.getLastSynced() : a.getCreatedAt();
                    Instant timeB = b.getLastSynced() != null ? b.getLastSynced() : b.getCreatedAt();
                    return timeA.compareTo(timeB);
                })
                .orElseThrow();

        return mapToResponse(mostRecent);
    }

    private KoreaderStatisticsResponse mapToResponse(KoreaderStatisticsEntity entity) {
        Instant lastOpenTime = null;
        if (entity.getLastOpenTime() != null) {
            lastOpenTime = Instant.ofEpochSecond(entity.getLastOpenTime());
        }

        return KoreaderStatisticsResponse.builder()
                .id(entity.getId())
                .bookId(entity.getBook() != null ? entity.getBook().getId() : null)
                .bookTitle(entity.getBookTitle())
                .bookAuthors(entity.getBookAuthors())
                .bookSeries(entity.getBookSeries())
                .bookLanguage(entity.getBookLanguage())
                .totalPages(entity.getTotalPages())
                .totalReadTime(entity.getTotalReadTime())
                .totalReadPages(entity.getTotalReadPages())
                .notesCount(entity.getNotesCount())
                .highlightsCount(entity.getHighlightsCount())
                .lastOpenTime(lastOpenTime)
                .lastSynced(entity.getLastSynced())
                .matchedToLibrary(entity.getBook() != null)
                .build();
    }

    // Inner class to hold book statistics
    private static class KoreaderBookStats {
        String title;
        String authors;
        Integer notes;
        Integer lastOpen;
        Integer highlights;
        Integer pages;
        String series;
        String language;
        String md5;
        Integer totalReadTime;
        Integer totalReadPages;
    }
}
