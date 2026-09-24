package org.example.soalabs.worker.api.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;

public final class WorkerPatchRequest {

    @Getter
    @Valid
    private OrganizationNamePatch organization;
    private boolean organizationPresent;

    @JsonSetter("organization")
    public void setOrganization(OrganizationNamePatch organization) {
        this.organization = organization;
        this.organizationPresent = true;
    }

    @AssertTrue(message = "organization must be present")
    public boolean isOrganizationPresent() {
        return organizationPresent;
    }
}
