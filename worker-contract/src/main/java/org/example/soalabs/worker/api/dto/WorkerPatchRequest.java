package org.example.soalabs.worker.api.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;

public final class WorkerPatchRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

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
