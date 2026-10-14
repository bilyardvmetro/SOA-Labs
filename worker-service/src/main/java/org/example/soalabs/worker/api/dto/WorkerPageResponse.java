package org.example.soalabs.worker.api.dto;

import java.util.List;

public record WorkerPageResponse(
        List<WorkerResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
