package org.example.soalabs.hr.api;

import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.soap.HrFault;
import org.example.soalabs.hr.soap.HrPort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/hr")
public class HrController {
    private final HrPort soap;

    public HrController(HrPort soap) {
        this.soap = soap;
    }

    @PostMapping("/fire/{workerId}")
    public WorkerDto fireWorker(@PathVariable int workerId, @RequestParam String organizationName) throws HrFault {
        return soap.fireWorker(workerId, organizationName).toDto();
    }

    @PostMapping("/index/{workerId}/{coeff}")
    public WorkerDto indexWorkerSalary(@PathVariable int workerId, @PathVariable BigDecimal coeff) throws HrFault {
        return soap.indexSalary(workerId, coeff).toDto();
    }
}
