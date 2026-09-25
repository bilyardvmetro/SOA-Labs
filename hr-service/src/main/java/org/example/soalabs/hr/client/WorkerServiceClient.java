package org.example.soalabs.hr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.example.soalabs.hr.api.dto.ErrorResponse;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.client.dto.WorkerUpdateRequest;
import org.example.soalabs.hr.service.EntityConflictException;
import org.example.soalabs.hr.service.UpstreamBadGatewayException;
import org.example.soalabs.hr.service.UpstreamUnavailableException;
import org.example.soalabs.hr.service.WorkerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkerServiceClient {

    private static final String INVALID_RESPONSE = "worker-service returned an invalid response";
    private static final String UNAVAILABLE = "worker-service is unavailable";

    private final RestClient workerServiceRestClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public WorkerDto getWorker(int workerId) {
        try {
            WorkerDto worker = workerServiceRestClient.get()
                    .uri("/workers/{id}", workerId)
                    .retrieve()
                    .body(WorkerDto.class);
            return validateResponse(worker, workerId);
        } catch (RestClientResponseException exception) {
            throw translateResponseException(exception, workerId);
        } catch (ResourceAccessException exception) {
            throw new UpstreamUnavailableException(UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new UpstreamBadGatewayException(INVALID_RESPONSE);
        }
    }

    public WorkerDto updateWorker(int workerId, WorkerUpdateRequest request) {
        try {
            WorkerDto worker = workerServiceRestClient.put()
                    .uri("/workers/{id}", workerId)
                    .body(request)
                    .retrieve()
                    .body(WorkerDto.class);
            return validateResponse(worker, workerId);
        } catch (RestClientResponseException exception) {
            throw translateResponseException(exception, workerId);
        } catch (ResourceAccessException exception) {
            throw new UpstreamUnavailableException(UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new UpstreamBadGatewayException(INVALID_RESPONSE);
        }
    }

    public WorkerDto fireWorker(int workerId) {
        try {
            WorkerDto worker = workerServiceRestClient.post()
                    .uri("/workers/{id}/fire", workerId)
                    .retrieve()
                    .body(WorkerDto.class);
            return validateResponse(worker, workerId);
        } catch (RestClientResponseException exception) {
            throw translateResponseException(exception, workerId);
        } catch (ResourceAccessException exception) {
            throw new UpstreamUnavailableException(UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new UpstreamBadGatewayException(INVALID_RESPONSE);
        }
    }

    private WorkerDto validateResponse(WorkerDto worker, int expectedId) {
        if (worker == null || !Integer.valueOf(expectedId).equals(worker.id())) {
            throw new UpstreamBadGatewayException(INVALID_RESPONSE);
        }
        Set<ConstraintViolation<WorkerDto>> violations = validator.validate(worker);
        if (!violations.isEmpty()) {
            throw new UpstreamBadGatewayException(INVALID_RESPONSE);
        }
        return worker;
    }

    private RuntimeException translateResponseException(RestClientResponseException exception, int workerId) {
        String message = upstreamMessage(exception);
        if (exception.getStatusCode().value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            return new WorkerNotFoundException(
                    message != null ? message : "Worker with id " + workerId + " does not exist"
            );
        }
        if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
            return new EntityConflictException(message != null ? message : "worker-service rejected the update");
        }
        return new UpstreamBadGatewayException(INVALID_RESPONSE);
    }

    private String upstreamMessage(RestClientResponseException exception) {
        try {
            ErrorResponse error = objectMapper.readValue(exception.getResponseBodyAsByteArray(), ErrorResponse.class);
            return error.message();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
