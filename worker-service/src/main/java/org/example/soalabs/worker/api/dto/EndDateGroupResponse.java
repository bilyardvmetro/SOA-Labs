package org.example.soalabs.worker.api.dto;

import java.time.Instant;

public record EndDateGroupResponse(Instant endDate, long count) {
}
