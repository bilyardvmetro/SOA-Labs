package org.example.soalabs.hr.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.soalabs.hr.service.UpstreamUnavailableException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConsulDiscovery {
    private final URI consul;
    private final RestClient client;
    private final AtomicInteger next = new AtomicInteger();

    public ConsulDiscovery(URI consul, RestClient client) {
        this.consul = consul;
        this.client = client;
    }

    public URI resolve() {
        try {
            JsonNode entries = client.get().uri(consul.resolve("/v1/health/service/worker-service?passing=true"))
                    .retrieve().body(JsonNode.class);

            if (entries == null || !entries.isArray() || entries.isEmpty()) throw new IllegalStateException();

            JsonNode entry = entries.get(Math.floorMod(next.getAndIncrement(), entries.size()));
            JsonNode service = entry.path("Service");
            String address = service.path("Address").asText();

            if (address.isBlank()) address = entry.path("Node").path("Address").asText();

            int port = service.path("Port").asInt();
            String scheme = service.path("Meta").path("scheme").asText("https");

            if (address.isBlank() || port < 1 || port > 65535 ||
                    !("http".equals(scheme) || "https".equals(scheme))) throw new IllegalStateException();

            return new URI(scheme, null, address, port, null, null, null);
        } catch (RuntimeException | URISyntaxException exception) {
            throw new UpstreamUnavailableException("worker-service discovery is unavailable");
        }
    }
}
