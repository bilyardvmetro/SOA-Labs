package org.example.soalabs.hr.config;

import java.net.URI;
import java.time.Duration;

public record WorkerServiceProperties(URI consulUrl, Duration connectTimeout, Duration readTimeout) {
    public static WorkerServiceProperties environment() {
        return new WorkerServiceProperties(
                URI.create(System.getenv().getOrDefault("CONSUL_HTTP_ADDR", "http://127.0.0.1:18500")),
                duration("WORKER_SERVICE_CONNECT_TIMEOUT", "3s"),
                duration("WORKER_SERVICE_READ_TIMEOUT", "5s"));
    }

    private static Duration duration(String key, String fallback) {
        String value = System.getenv().getOrDefault(key, fallback);
        Duration parsed = value.startsWith("P") ? Duration.parse(value)
                : value.endsWith("ms") ? Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)))
                : Duration.ofSeconds(Long.parseLong(value.replaceFirst("s$", "")));
        if (parsed.isZero() || parsed.isNegative()) throw new IllegalArgumentException(key + " must be positive");
        return parsed;
    }
}
