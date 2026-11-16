package org.example.soalabs.hr.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.math.BigDecimal;

@WebService(name = "HrPortType", targetNamespace = HrPort.NAMESPACE)
public interface HrPort {
    String NAMESPACE = "urn:soa-labs:hr:v1";

    @WebMethod
    @WebResult(name = "worker")
    SoapWorker fireWorker(@WebParam(name = "workerId") int workerId,
                          @WebParam(name = "organizationName") String organizationName) throws HrFault;

    @WebMethod
    @WebResult(name = "worker")
    SoapWorker indexSalary(@WebParam(name = "workerId") int workerId,
                           @WebParam(name = "coefficient") BigDecimal coefficient) throws HrFault;
}
