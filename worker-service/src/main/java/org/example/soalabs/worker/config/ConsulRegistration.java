package org.example.soalabs.worker.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.util.Map;

@Component
@EnableScheduling
public class ConsulRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(ConsulRegistration.class);
    private final RestClient consul;
    private final String id;
    private final Map<String, Object> registration;
    private volatile boolean ready;

    public ConsulRegistration(Environment env) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);

        consul = RestClient.builder().baseUrl(env.getProperty("CONSUL_HTTP_ADDR", "http://127.0.0.1:18500"))
                .requestFactory(factory).build();

        id = env.getProperty("WORKER_INSTANCE_ID", "worker-1");

        URI publicUrl = URI.create(env.getProperty("WORKER_PUBLIC_URL", "https://127.0.0.1:18443"));
        if (publicUrl.getHost() == null || publicUrl.getPort() < 1 ||
                !("http".equals(publicUrl.getScheme()) || "https".equals(publicUrl.getScheme()))) {
            throw new IllegalArgumentException("WORKER_PUBLIC_URL must specify HTTP(S) host and port");
        }

        registration = Map.of(
                "ID", id, "Name", "worker-service",
                "Address", publicUrl.getHost(), "Port", publicUrl.getPort(),
                "Meta", Map.of("scheme", publicUrl.getScheme()),
                "Check", Map.of(
                        "HTTP", publicUrl.resolve("/health").toString(),
                        "Interval", "15s", "Timeout", "5s",
                        "DeregisterCriticalServiceAfter", "2m"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void started() {
        ready = true;
        register();
    }

    // Re-register after an agent restart or critical-service eviction.
    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public synchronized void register() {
        if (!ready) return;
        try {
            consul.put().uri("/v1/agent/service/register").body(registration).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            LOG.warn("Consul registration failed; retrying in 30 seconds ({})", exception.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public synchronized void stopped() {
        ready = false;
        try {
            consul.put().uri("/v1/agent/service/deregister/{id}", id).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            LOG.warn("Consul deregistration failed; health check will expire the service");
        }
    }
}
