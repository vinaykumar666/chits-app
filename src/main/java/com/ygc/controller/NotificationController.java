package com.ygc.controller;

import com.ygc.service.NotificationService;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE + notification history endpoints.
 *
 * GET /api/notifications/subscribe  — long-lived SSE stream
 * GET /api/notifications/history    — FIX: returns stored notifications as JSON
 *                                     (bell panel loads this instead of relying
 *                                      on the JS in-memory log that resets on navigation)
 * DELETE /api/notifications/history — FIX: clear history for the current user
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final LoggingUtil loggingUtil;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        String userEmail = authentication.getName();
        loggingUtil.apiCall("GET", "/api/notifications/subscribe", "NotificationController");
        return notificationService.subscribe(userEmail);
    }

    /**
     * FIX: Bell panel calls this to load notifications that arrived in previous
     * sessions or while the user was on a different page.
     */
    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> history(Authentication authentication) {
        String userEmail = authentication.getName();
        String json = notificationService.getHistoryJson(userEmail);
        return ResponseEntity.ok(json);
    }

    /**
     * FIX: Called by the "Clear all" button in the bell panel.
     */
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(Authentication authentication) {
        notificationService.clearHistory(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
