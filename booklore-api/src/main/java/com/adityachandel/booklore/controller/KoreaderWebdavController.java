package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.config.security.service.AuthenticationService;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.repository.UserRepository;
import com.adityachandel.booklore.service.koreader.KoreaderStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/koreader/webdav")
@Tag(name = "KoReader WebDAV", description = "WebDAV endpoint for KOReader statistics synchronization")
public class KoreaderWebdavController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final KoreaderStatisticsService koreaderStatisticsService;

    @Operation(summary = "WebDAV OPTIONS", description = "Handle WebDAV OPTIONS request")
    @ApiResponse(responseCode = "200", description = "WebDAV capabilities returned")
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok()
                .header("DAV", "1, 2")
                .header("Allow", "OPTIONS, GET, HEAD, PUT, PROPFIND")
                .build();
    }

    @Operation(summary = "WebDAV PROPFIND", description = "Handle WebDAV PROPFIND request for directory listing")
    @ApiResponse(responseCode = "207", description = "Multi-status response with directory listing")
    @RequestMapping(value = "/**")
    public ResponseEntity<String> propfind(HttpServletRequest request) {
        // Handle PROPFIND method
        if (!"PROPFIND".equalsIgnoreCase(request.getMethod())) {
            return null; // Let other methods handle it
        }
        
        log.debug("PROPFIND request for path: {}", request.getRequestURI());

        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <D:multistatus xmlns:D="DAV:">
                    <D:response>
                        <D:href>%s</D:href>
                        <D:propstat>
                            <D:prop>
                                <D:resourcetype>
                                    <D:collection/>
                                </D:resourcetype>
                            </D:prop>
                            <D:status>HTTP/1.1 200 OK</D:status>
                        </D:propstat>
                    </D:response>
                </D:multistatus>
                """.formatted(request.getRequestURI());

        return ResponseEntity.status(207)
                .header("Content-Type", "application/xml; charset=utf-8")
                .body(xml);
    }

    @Operation(summary = "Upload KOReader statistics", description = "Upload KOReader statistics.sqlite file via WebDAV PUT")
    @ApiResponse(responseCode = "201", description = "File uploaded and processed successfully")
    @ApiResponse(responseCode = "204", description = "File uploaded successfully")
    @PutMapping("/{filename}")
    public ResponseEntity<Map<String, String>> uploadStatistics(
            @Parameter(description = "Filename (should be statistics.sqlite)") @PathVariable String filename,
            HttpServletRequest request) {
        
        log.info("Received WebDAV PUT request for file: {}", filename);

        try {
            BookLoreUser authenticatedUser = authenticationService.getAuthenticatedUser();
            Long userId = authenticatedUser.getId();
            
            BookLoreUserEntity userEntity = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

            // Create temporary directory for user's statistics
            Path tempDir = Files.createTempDirectory("koreader-stats-" + userId);
            File tempFile = new File(tempDir.toFile(), filename);

            // Save uploaded file
            try (InputStream inputStream = request.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                log.info("Saved {} bytes to temporary file: {}", totalBytes, tempFile.getAbsolutePath());
            }

            // Process the statistics database
            if (filename.toLowerCase().endsWith(".sqlite") || filename.toLowerCase().contains("statistics")) {
                koreaderStatisticsService.processStatisticsDatabase(tempFile, userEntity);
                log.info("Successfully processed statistics database for user: {}", userId);
            }

            // Clean up temporary file
            try {
                Files.deleteIfExists(tempFile.toPath());
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                log.warn("Failed to delete temporary files", e);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "Statistics processed successfully"));

        } catch (IOException e) {
            log.error("Failed to upload statistics file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to process file: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get file via WebDAV", description = "Handle WebDAV GET request")
    @ApiResponse(responseCode = "404", description = "File not found")
    @GetMapping("/{filename}")
    public ResponseEntity<String> getFile(@PathVariable String filename) {
        log.debug("GET request for file: {}", filename);
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "WebDAV HEAD request", description = "Handle WebDAV HEAD request")
    @ApiResponse(responseCode = "200", description = "File exists")
    @ApiResponse(responseCode = "404", description = "File not found")
    @RequestMapping(method = RequestMethod.HEAD, value = "/{filename}")
    public ResponseEntity<Void> head(@PathVariable String filename) {
        log.debug("HEAD request for file: {}", filename);
        // Return 200 to indicate the endpoint is available
        return ResponseEntity.ok().build();
    }
}
