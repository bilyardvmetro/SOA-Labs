package org.example.soalabs.worker.service;

import jakarta.ejb.Remote;
import org.example.soalabs.worker.api.dto.*;
import org.example.soalabs.worker.domain.WorkerStatus;
import java.util.List;

@Remote
public interface WorkerRemote {
    WorkerPageResponse getWorkers(int page, int size, List<String> sort, List<String> filters);
    WorkerResponse getWorker(int id);
    WorkerResponse createWorker(WorkerInput input);
    WorkerResponse updateWorker(int id, WorkerInput input);
    WorkerResponse patchWorker(int id, WorkerPatchRequest patch);
    void deleteWorker(int id);
    WorkerResponse fireWorker(int id);
    List<EndDateGroupResponse> countGroupedByEndDate();
    WorkerPageResponse findByNamePrefix(String prefix, int page, int size, List<String> sort);
    WorkerPageResponse findByStatusGreaterThan(WorkerStatus status, int page, int size, List<String> sort);
    SumResult sumSalaries();
    CountResult countByEndDate(String endDate);
    CountResult countSalaryLessThan(int threshold);
    void health();
}
