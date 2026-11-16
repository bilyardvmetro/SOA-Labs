package org.example.soalabs.hr.soap;

import jakarta.ejb.EJB;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.soap.SOAPBinding;
import org.example.soalabs.hr.api.dto.WorkerDto;
import org.example.soalabs.hr.service.EntityConflictException;
import org.example.soalabs.hr.service.HrRemote;
import org.example.soalabs.hr.service.UpstreamBadGatewayException;
import org.example.soalabs.hr.service.UpstreamUnavailableException;
import org.example.soalabs.hr.service.WorkerNotFoundException;

import java.math.BigDecimal;
import java.util.function.Supplier;

@WebService(
        endpointInterface = "org.example.soalabs.hr.soap.HrPort",
        targetNamespace = HrPort.NAMESPACE,
        serviceName = "HrService",
        portName = "HrPort"
)
@BindingType(SOAPBinding.SOAP11HTTP_BINDING)
public class HrSoapEndpoint implements HrPort {
    @EJB(lookup = "java:global/hr/hr-ejb/HrService!org.example.soalabs.hr.service.HrRemote")
    private HrRemote service;

    @Override
    public SoapWorker fireWorker(int workerId, String organizationName) throws HrFault {
        validateWorkerId(workerId);
        if (organizationName == null || organizationName.isBlank() || organizationName.length() > 1804) {
            throw new HrFault(HrFault.Code.INVALID_REQUEST, "organizationName must contain 1 to 1804 characters");
        }
        return invoke(() -> service.fireWorker(workerId, organizationName));
    }

    @Override
    public SoapWorker indexSalary(int workerId, BigDecimal coefficient) throws HrFault {
        validateWorkerId(workerId);
        if (coefficient == null || coefficient.signum() <= 0) {
            throw new HrFault(HrFault.Code.INVALID_REQUEST, "coeff must be greater than 0");
        }
        return invoke(() -> service.indexSalary(workerId, coefficient));
    }

    private static void validateWorkerId(int workerId) throws HrFault {
        if (workerId <= 0) {
            throw new HrFault(HrFault.Code.INVALID_REQUEST, "workerId must be greater than or equal to 1");
        }
    }

    private static SoapWorker invoke(Supplier<WorkerDto> operation) throws HrFault {
        try {
            return SoapWorker.from(operation.get());
        } catch (WorkerNotFoundException exception) {
            throw new HrFault(HrFault.Code.WORKER_NOT_FOUND, exception.getMessage());
        } catch (EntityConflictException exception) {
            throw new HrFault(HrFault.Code.CONFLICT, exception.getMessage());
        } catch (UpstreamBadGatewayException exception) {
            throw new HrFault(HrFault.Code.UPSTREAM_BAD_GATEWAY, exception.getMessage());
        } catch (UpstreamUnavailableException exception) {
            throw new HrFault(HrFault.Code.UPSTREAM_UNAVAILABLE, exception.getMessage());
        }
    }
}
