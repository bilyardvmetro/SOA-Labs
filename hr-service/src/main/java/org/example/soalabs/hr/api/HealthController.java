package org.example.soalabs.hr.api;

import lombok.RequiredArgsConstructor;
import org.example.soalabs.hr.service.HrRemote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final HrRemote service;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            service.health();
            return ResponseEntity.ok(Map.of("status", "UP", "node", System.getenv().getOrDefault("NODE_ID", "hr")));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
