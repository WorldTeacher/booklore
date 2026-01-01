-- Create KOReader statistics table
CREATE TABLE IF NOT EXISTS koreader_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT,
    koreader_book_md5 VARCHAR(32) NOT NULL,
    book_title VARCHAR(500),
    book_authors VARCHAR(1000),
    book_series VARCHAR(500),
    book_language VARCHAR(50),
    total_pages INT,
    total_read_time INT,
    total_read_pages INT,
    notes_count INT,
    highlights_count INT,
    last_open_time INT,
    last_synced DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_koreader_statistics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_koreader_statistics_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE SET NULL,
    UNIQUE KEY unique_user_book_md5 (user_id, koreader_book_md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create KOReader page statistics table
CREATE TABLE IF NOT EXISTS koreader_page_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    koreader_statistics_id BIGINT NOT NULL,
    page INT NOT NULL,
    start_time INT NOT NULL,
    duration INT NOT NULL,
    total_pages_at_time INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_page_stats_statistics FOREIGN KEY (koreader_statistics_id) REFERENCES koreader_statistics(id) ON DELETE CASCADE,
    INDEX idx_koreader_statistics_id (koreader_statistics_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better query performance
CREATE INDEX idx_koreader_stats_user ON koreader_statistics(user_id);
CREATE INDEX idx_koreader_stats_book ON koreader_statistics(book_id);
CREATE INDEX idx_koreader_stats_md5 ON koreader_statistics(koreader_book_md5);
CREATE INDEX idx_koreader_stats_last_synced ON koreader_statistics(last_synced);
