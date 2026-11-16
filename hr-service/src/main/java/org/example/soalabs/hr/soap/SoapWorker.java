package org.example.soalabs.hr.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.example.soalabs.hr.api.dto.CoordinatesDto;
import org.example.soalabs.hr.api.dto.OrganizationDto;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.domain.OrganizationType;
import org.example.soalabs.hr.domain.WorkerStatus;

import java.time.Instant;

@XmlType(name = "Worker", propOrder = {
        "id", "name", "coordinates", "creationDate", "salary", "startDate", "endDate", "status", "organization"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapWorker {
    @XmlElement(required = true)
    private Integer id;
    @XmlElement(required = true)
    private String name;
    @XmlElement(required = true)
    private Coordinates coordinates;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private Instant creationDate;
    private Integer salary;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private Instant startDate;
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private Instant endDate;
    @XmlElement(required = true)
    private WorkerStatus status;
    private Organization organization;

    static SoapWorker from(WorkerDto worker) {
        SoapWorker result = new SoapWorker();
        result.id = worker.id();
        result.name = worker.name();
        result.coordinates = Coordinates.from(worker.coordinates());
        result.creationDate = worker.creationDate();
        result.salary = worker.salary();
        result.startDate = worker.startDate();
        result.endDate = worker.endDate();
        result.status = worker.status();
        result.organization = worker.organization() == null ? null : Organization.from(worker.organization());
        return result;
    }

    @XmlType(name = "Coordinates", propOrder = {"x", "y"})
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Coordinates {
        @XmlElement(required = true)
        private Integer x;
        @XmlElement(required = true)
        private Float y;

        private static Coordinates from(CoordinatesDto coordinates) {
            Coordinates result = new Coordinates();
            result.x = coordinates.x();
            result.y = coordinates.y();
            return result;
        }
    }

    @XmlType(name = "Organization", propOrder = {"fullName", "annualTurnover", "type"})
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Organization {
        @XmlElement(required = true)
        private String fullName;
        @XmlElement(required = true)
        private Integer annualTurnover;
        @XmlElement(required = true)
        private OrganizationType type;

        private static Organization from(OrganizationDto organization) {
            Organization result = new Organization();
            result.fullName = organization.fullName();
            result.annualTurnover = organization.annualTurnover();
            result.type = organization.type();
            return result;
        }
    }

    public static class InstantAdapter extends XmlAdapter<String, Instant> {
        @Override
        public Instant unmarshal(String value) {
            return Instant.parse(value);
        }

        @Override
        public String marshal(Instant value) {
            return value.toString();
        }
    }
}
