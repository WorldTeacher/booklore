package com.adityachandel.booklore.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KoreaderStatisticsResponse {
    
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthors;
    private String bookSeries;
    private String bookLanguage;
    private Integer totalPages;
    private Integer totalReadTime;
    private Integer totalReadPages;
    private Integer notesCount;
    private Integer highlightsCount;
    private Instant lastOpenTime;
    private Instant lastSynced;
    private Boolean matchedToLibrary;
}
