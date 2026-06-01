package com.ygc.controller;

import com.ygc.service.NotificationService;
import com.ygc.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint: clients open GET /api/notifications/subscribe once after login.
 * The connection stays open; the server pushes JSON events as they occur.
 *
 * JavaScript example:
 *   const es = new EventSource('/api/notifications/subscribe');
 *   es.addEventListener('PAYMENT_REMINDER', e => showToast(JSON.parse(e.data)));
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final LoggingUtil loggingUtil;

    /**
     * Subscribe to the SSE push stream.
     * Spring Security ensures only authenticated users reach this endpoint.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        String userEmail = authentication.getName();
        loggingUtil.apiCall("GET", "/api/notifications/subscribe", "NotificationController");
        return notificationService.subscribe(userEmail);
    }
}
