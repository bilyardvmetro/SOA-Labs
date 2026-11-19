package org.example.soalabs.hr.config;

import java.net.URI;
import java.time.Duration;

public record WorkerServiceProperties(URI esbUrl, Duration connectTimeout, Duration readTimeout) {
    public static WorkerServiceProperties environment() {
        String endpoint = System.getenv("WORKER_ESB_URL");
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("WORKER_ESB_URL is required");
        }
        URI esbUrl = URI.create(endpoint);
        if (esbUrl.getHost() == null || esbUrl.getPort() < 1 || esbUrl.getPort() > 65535
                || !("http".equals(esbUrl.getScheme()) || "https".equals(esbUrl.getScheme()))
                || esbUrl.getRawUserInfo() != null || esbUrl.getRawQuery() != null
                || esbUrl.getRawFragment() != null
                || !(esbUrl.getPath().isEmpty() || "/".equals(esbUrl.getPath()))) {
            throw new IllegalStateException("WORKER_ESB_URL must specify an HTTP(S) origin with an explicit port");
        }
        return new WorkerServiceProperties(
                esbUrl,
                duration("WORKER_SERVICE_CONNECT_TIMEOUT", "3s"),
                duration("WORKER_SERVICE_READ_TIMEOUT", "8s"));
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
