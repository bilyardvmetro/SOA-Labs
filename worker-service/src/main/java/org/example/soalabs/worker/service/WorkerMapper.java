package org.example.soalabs.worker.service;

import org.example.soalabs.worker.api.dto.CoordinatesDto;
import org.example.soalabs.worker.api.dto.OrganizationDto;
import org.example.soalabs.worker.api.dto.WorkerInput;
import org.example.soalabs.worker.api.dto.WorkerResponse;
import org.example.soalabs.worker.domain.Coordinates;
import org.example.soalabs.worker.domain.Organization;
import org.example.soalabs.worker.domain.Worker;
import org.springframework.stereotype.Component;

@Component
public class WorkerMapper {

    public Worker toEntity(WorkerInput input) {
        return new Worker(
                input.name(),
                toCoordinates(input.coordinates()),
                input.salary(),
                input.startDate(),
                input.endDate(),
                input.status(),
                toOrganization(input.organization())
        );
    }

    public void replace(Worker worker, WorkerInput input) {
        worker.replace(
                input.name(),
                toCoordinates(input.coordinates()),
                input.salary(),
                input.startDate(),
                input.endDate(),
                input.status(),
                toOrganization(input.organization())
        );
    }

    public WorkerResponse toResponse(Worker worker) {
        Coordinates coordinates = worker.getCoordinates();
        Organization organization = worker.getOrganization();

        return new WorkerResponse(
                worker.getId(),
                worker.getName(),
                new CoordinatesDto(coordinates.getX(), coordinates.getY()),
                worker.getCreationDate(),
                worker.getSalary(),
                worker.getStartDate(),
                worker.getEndDate(),
                worker.getStatus(),
                organization == null ? null : new OrganizationDto(
                        organization.getFullName(),
                        organization.getAnnualTurnover(),
                        organization.getType()
                )
        );
    }

    private Coordinates toCoordinates(CoordinatesDto coordinates) {
        return new Coordinates(coordinates.x(), coordinates.y());
    }

    private Organization toOrganization(OrganizationDto organization) {
        if (organization == null) {
            return null;
        }
        return new Organization(organization.fullName(), organization.annualTurnover(), organization.type());
    }
}
