package org.example.soalabs.hr.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.service.HrService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService service;

    @PostMapping("/fire/{workerId}")
    public WorkerDto fireWorker(
            @PathVariable @Min(1) int workerId,
            @RequestParam @NotBlank @Size(max = 1804) String organizationName
    ) {
        return service.fireWorker(workerId, organizationName);
    }

    @PostMapping("/index/{workerId}/{coeff}")
    public WorkerDto indexWorkerSalary(
            @PathVariable @Min(1) int workerId,
            @PathVariable @DecimalMin(value = "0", inclusive = false) BigDecimal coeff
    ) {
        return service.indexSalary(workerId, coeff);
    }
}
