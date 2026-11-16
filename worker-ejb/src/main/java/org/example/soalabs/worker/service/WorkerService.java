package org.example.soalabs.worker.service;

import org.example.soalabs.worker.api.dto.CountResult;
import org.example.soalabs.worker.api.dto.EndDateGroupResponse;
import org.example.soalabs.worker.api.dto.SumResult;
import org.example.soalabs.worker.api.dto.WorkerInput;
import org.example.soalabs.worker.api.dto.WorkerPageResponse;
import org.example.soalabs.worker.api.dto.WorkerPatchRequest;
import org.example.soalabs.worker.api.dto.WorkerResponse;
import org.example.soalabs.worker.domain.Worker;
import org.example.soalabs.worker.domain.WorkerStatus;
import org.example.soalabs.worker.repository.WorkerRepository;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;



import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Stateless(name = "WorkerService")
@jakarta.interceptor.Interceptors(PersistenceErrors.class)
public class WorkerService implements WorkerRemote {

    @PersistenceContext(unitName = "workers")
    private EntityManager entityManager;
    private WorkerRepository repository;
    private final WorkerMapper mapper = new WorkerMapper();
    private final WorkerQueryParser queryParser = new WorkerQueryParser();

    @PostConstruct
    void initialize() { repository = new WorkerRepository(entityManager); }

    public void health() { entityManager.createNativeQuery("select 1").getSingleResult(); }

    public WorkerPageResponse getWorkers(int page, int size, List<String> sortExpressions,
                                         List<String> filterExpressions) {
        Pageable pageable = pageable(page, size, sortExpressions);
        Specification<Worker> specification = queryParser.parseFilters(filterExpressions);
        return toPage(repository.findAll(specification, pageable));
    }

    public WorkerResponse getWorker(int id) {
        return mapper.toResponse(requireWorker(id));
    }

    public WorkerResponse createWorker(WorkerInput input) {
        requireEditableStatus(input.status());
        validateNewDate("startDate", input.startDate());
        validateNewDate("endDate", input.endDate());
        return mapper.toResponse(repository.save(mapper.toEntity(input)));
    }

    public WorkerResponse updateWorker(int id, WorkerInput input) {
        Worker worker = requireWorker(id);
        requireActive(worker);
        requireEditableStatus(input.status());
        validateChangedDate("startDate", worker.getStartDate(), input.startDate());
        validateChangedDate("endDate", worker.getEndDate(), input.endDate());
        mapper.replace(worker, input);
        return mapper.toResponse(worker);
    }

    public WorkerResponse patchWorker(int id, WorkerPatchRequest patch) {
        Worker worker = requireWorker(id);
        requireActive(worker);
        if (patch.getOrganization() == null) {
            worker.removeOrganization();
        } else {
            if (worker.getOrganization() == null) {
                throw new EntityConflictException("Worker with id " + id + " has no organization to rename");
            }
            worker.getOrganization().rename(patch.getOrganization().fullName());
        }
        return mapper.toResponse(worker);
    }

    public void deleteWorker(int id) {
        Worker worker = requireWorker(id);
        repository.delete(worker);
    }

    public WorkerResponse fireWorker(int id) {
        Worker worker = requireWorker(id);
        worker.fire();
        return mapper.toResponse(worker);
    }

    public List<EndDateGroupResponse> countGroupedByEndDate() {
        return repository.countGroupedByEndDate().stream()
                .map(row -> new EndDateGroupResponse((Instant) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public WorkerPageResponse findByNamePrefix(String prefix, int page, int size, List<String> sortExpressions) {
        return toPage(repository.findByNameStartingWith(prefix, pageable(page, size, sortExpressions)));
    }

    public WorkerPageResponse findByStatusGreaterThan(WorkerStatus status, int page, int size,
                                                       List<String> sortExpressions) {
        List<WorkerStatus> greaterStatuses = Arrays.stream(WorkerStatus.values())
                .filter(candidate -> candidate.ordinal() > status.ordinal())
                .toList();
        Specification<Worker> specification = greaterStatuses.isEmpty()
                ? (root, query, cb) -> cb.disjunction()
                : (root, query, cb) -> root.get("status").in(greaterStatuses);
        return toPage(repository.findAll(specification, pageable(page, size, sortExpressions)));
    }

    public SumResult sumSalaries() {
        Long sum = repository.sumSalaries();
        return new SumResult(sum == null ? 0 : sum);
    }

    public CountResult countByEndDate(String endDate) {
        if ("null".equals(endDate)) {
            return new CountResult(repository.countByEndDateIsNull());
        }
        try {
            return new CountResult(repository.countByEndDate(Instant.parse(endDate)));
        } catch (DateTimeParseException exception) {
            throw new InvalidRequestException("endDate must be an ISO 8601 date-time or null");
        }
    }

    public CountResult countSalaryLessThan(int threshold) {
        return new CountResult(repository.countBySalaryLessThan(threshold));
    }

    private Pageable pageable(int page, int size, List<String> sortExpressions) {
        Sort sort = queryParser.parseSort(sortExpressions);
        return PageRequest.of(page - 1, size, sort);
    }

    private WorkerPageResponse toPage(Page<Worker> workers) {
        return new WorkerPageResponse(
                workers.getContent().stream().map(mapper::toResponse).toList(),
                workers.getNumber() + 1,
                workers.getSize(),
                workers.getTotalElements(),
                workers.getTotalPages()
        );
    }

    private Worker requireWorker(int id) {
        return repository.findById(id).orElseThrow(() -> new WorkerNotFoundException(id));
    }

    private void requireActive(Worker worker) {
        if (worker.getStatus() == WorkerStatus.FIRED) {
            throw new EntityConflictException("Fired worker cannot be modified");
        }
    }

    private void requireEditableStatus(WorkerStatus status) {
        if (status == WorkerStatus.FIRED) {
            throw new EntityConflictException("FIRED status can only be assigned through hr-service");
        }
    }

    private void validateNewDate(String field, Instant value) {
        if (value != null && value.isBefore(Instant.now())) {
            throw new EntityConflictException(field + " must not be in the past");
        }
    }

    private void validateChangedDate(String field, Instant currentValue, Instant requestedValue) {
        if (!Objects.equals(currentValue, requestedValue)) {
            validateNewDate(field, requestedValue);
        }
    }
}
