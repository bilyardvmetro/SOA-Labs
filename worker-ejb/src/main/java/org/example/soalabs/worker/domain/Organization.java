package org.example.soalabs.worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization {

    @Column(name = "organization_full_name", length = 1804)
    private String fullName;

    @Column(name = "organization_annual_turnover")
    private Integer annualTurnover;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 40)
    private OrganizationType type;

    public void rename(String fullName) {
        this.fullName = fullName;
    }
}
