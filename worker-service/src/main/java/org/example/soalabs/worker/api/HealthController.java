package org.example.soalabs.worker.api;

import lombok.RequiredArgsConstructor;
import org.example.soalabs.worker.service.WorkerRemote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final WorkerRemote service;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            service.health();
            return ResponseEntity.ok(Map.of("status", "UP", "node", System.getenv().getOrDefault("NODE_ID", "worker")));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
