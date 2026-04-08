package com.artezo.controller;

import com.artezo.dto.response.NotificationAlertDTO;
import com.artezo.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── GET alerts — pass adminId from session/auth ──────────────
    @GetMapping("/alerts")
    public ResponseEntity<NotificationAlertDTO> getAlerts(
            @RequestParam String adminId) {             // or extract from JWT/session
        return ResponseEntity.ok(notificationService.getAlerts(adminId));
    }

    // ── POST visit — called when Visit button clicked ────────────
    @PostMapping("/visit")
    public ResponseEntity<Void> markVisited(
            @RequestParam String adminId,
            @RequestParam String fingerprint) {
        notificationService.markVisited(adminId, fingerprint);
        return ResponseEntity.ok().build();
    }

    // ── POST mark-all-read — called when bell "mark all" clicked ─
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(
            @RequestParam String adminId) {
        notificationService.markAllVisited(adminId);
        return ResponseEntity.ok().build();
    }
}