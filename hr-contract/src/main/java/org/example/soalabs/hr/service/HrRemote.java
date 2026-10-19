package org.example.soalabs.hr.service;

import jakarta.ejb.Remote;
import org.example.soalabs.hr.api.dto.WorkerDto;
import java.math.BigDecimal;

@Remote
public interface HrRemote {
    WorkerDto fireWorker(int workerId, String organizationName);
    WorkerDto indexSalary(int workerId, BigDecimal coefficient);
    void health();
}
