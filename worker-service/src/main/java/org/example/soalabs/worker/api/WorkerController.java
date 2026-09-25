package org.example.soalabs.worker.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.soalabs.worker.api.dto.CountResult;
import org.example.soalabs.worker.api.dto.EndDateGroupResponse;
import org.example.soalabs.worker.api.dto.SumResult;
import org.example.soalabs.worker.api.dto.WorkerInput;
import org.example.soalabs.worker.api.dto.WorkerPageResponse;
import org.example.soalabs.worker.api.dto.WorkerPatchRequest;
import org.example.soalabs.worker.api.dto.WorkerResponse;
import org.example.soalabs.worker.domain.WorkerStatus;
import org.example.soalabs.worker.service.WorkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Validated
@RestController
@RequestMapping("/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService service;

    @GetMapping
    public WorkerPageResponse getWorkers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest request
    ) {
        return service.getWorkers(page, size, values(request, "sort"), values(request, "filter"));
    }

    @PostMapping
    public ResponseEntity<WorkerResponse> createWorker(@Valid @RequestBody WorkerInput input) {
        WorkerResponse worker = service.createWorker(input);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(worker.id())
                .toUri();
        return ResponseEntity.created(location).body(worker);
    }

    @GetMapping("/{id}")
    public WorkerResponse getWorker(@PathVariable @Min(1) int id) {
        return service.getWorker(id);
    }

    @PutMapping("/{id}")
    public WorkerResponse updateWorker(@PathVariable @Min(1) int id, @Valid @RequestBody WorkerInput input) {
        return service.updateWorker(id, input);
    }

    @PatchMapping("/{id}")
    public WorkerResponse patchWorker(@PathVariable @Min(1) int id,
                                      @Valid @RequestBody WorkerPatchRequest patch) {
        return service.patchWorker(id, patch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(@PathVariable @Min(1) int id) {
        service.deleteWorker(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fire")
    public WorkerResponse fireWorker(@PathVariable @Min(1) int id) {
        return service.fireWorker(id);
    }

    @GetMapping("/group-by-end-date")
    public List<EndDateGroupResponse> countWorkersGroupedByEndDate() {
        return service.countGroupedByEndDate();
    }

    @GetMapping("/search/by-name-prefix")
    public WorkerPageResponse findWorkersByNamePrefix(
            @RequestParam @NotBlank String prefix,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest request
    ) {
        return service.findByNamePrefix(prefix, page, size, values(request, "sort"));
    }

    @GetMapping("/search/by-status-greater-than")
    public WorkerPageResponse findWorkersByStatusGreaterThan(
            @RequestParam WorkerStatus status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest request
    ) {
        return service.findByStatusGreaterThan(status, page, size, values(request, "sort"));
    }

    @GetMapping("/sum/salary")
    public SumResult sumSalaries() {
        return service.sumSalaries();
    }

    @GetMapping("/count/endDate/{endDate}")
    public CountResult countByEndDate(@PathVariable String endDate) {
        return service.countByEndDate(endDate);
    }

    @GetMapping("/count/salary/less/{threshold}")
    public CountResult countSalaryLessThan(@PathVariable @Min(1) int threshold) {
        return service.countSalaryLessThan(threshold);
    }

    private List<String> values(HttpServletRequest request, String parameter) {
        String[] values = request.getParameterValues(parameter);
        return values == null ? List.of() : Arrays.asList(values);
    }
}
