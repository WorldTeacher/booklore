package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.progress.KoreaderProgress;
import com.adityachandel.booklore.model.dto.response.KoreaderStatisticsResponse;
import com.adityachandel.booklore.service.koreader.KoreaderService;
import com.adityachandel.booklore.service.koreader.KoreaderStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/koreader")
@Tag(name = "KoReader", description = "Endpoints for KoReader device integration and progress sync")
public class KoreaderController {

    private final KoreaderService koreaderService;
    private final KoreaderStatisticsService koreaderStatisticsService;

    @Operation(summary = "Authorize KoReader user", description = "Authorize a user for KoReader sync.")
    @ApiResponse(responseCode = "200", description = "User authorized successfully")
    @GetMapping("/users/auth")
    public ResponseEntity<Map<String, String>> authorizeUser() {
        return koreaderService.authorizeUser();
    }

    @Operation(summary = "Create KoReader user (disabled)", description = "Attempt to register a user via KoReader (always forbidden).")
    @ApiResponse(responseCode = "403", description = "User registration forbidden")
    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@Parameter(description = "User data") @RequestBody Map<String, Object> userData) {
        log.warn("Attempt to register user via Koreader blocked: {}", userData);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "User registration via Koreader is disabled"));
    }

    @Operation(summary = "Get KoReader progress", description = "Retrieve reading progress for a book by its hash.")
    @ApiResponse(responseCode = "200", description = "Progress returned successfully")
    @GetMapping("/syncs/progress/{bookHash}")
    public ResponseEntity<KoreaderProgress> getProgress(@Parameter(description = "Book hash") @PathVariable String bookHash) {
        KoreaderProgress progress = koreaderService.getProgress(bookHash);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(progress);
    }

    @Operation(summary = "Update KoReader progress", description = "Update reading progress for a book.")
    @ApiResponse(responseCode = "200", description = "Progress updated successfully")
    @PutMapping("/syncs/progress")
    public ResponseEntity<?> updateProgress(@Parameter(description = "KoReader progress object") @Valid @RequestBody KoreaderProgress koreaderProgress) {
        koreaderService.saveProgress(koreaderProgress.getDocument(), koreaderProgress);
        return ResponseEntity.ok(Map.of("status", "progress updated"));
    }

    @Operation(summary = "Get KOReader statistics", description = "Retrieve all KOReader statistics for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping("/statistics")
    public ResponseEntity<List<KoreaderStatisticsResponse>> getStatistics() {
        List<KoreaderStatisticsResponse> statistics = koreaderStatisticsService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "Get KOReader statistics for a book", description = "Retrieve KOReader statistics for a specific book.")
    @ApiResponse(responseCode = "200", description = "Book statistics retrieved successfully")
    @GetMapping("/statistics/book/{bookId}")
    public ResponseEntity<KoreaderStatisticsResponse> getBookStatistics(@Parameter(description = "Book ID") @PathVariable Long bookId) {
        KoreaderStatisticsResponse statistics = koreaderStatisticsService.getBookStatistics(bookId);
        return ResponseEntity.ok(statistics);
    }
}
