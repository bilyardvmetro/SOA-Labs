package org.example.soalabs.hr.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.ws.WebFault;

@WebFault(name = "HrFault", targetNamespace = HrPort.NAMESPACE)
public class HrFault extends Exception {
    private final Detail faultInfo;

    public HrFault(String message, Detail faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    HrFault(Code code, String message) {
        this(message, new Detail(code));
    }

    public Detail getFaultInfo() {
        return faultInfo;
    }

    public enum Code {
        INVALID_REQUEST, WORKER_NOT_FOUND, CONFLICT, UPSTREAM_BAD_GATEWAY, UPSTREAM_UNAVAILABLE
    }

    @XmlType(name = "HrFaultDetail")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Detail {
        @XmlElement(required = true)
        private Code code;

        public Detail() {
        }

        private Detail(Code code) {
            this.code = code;
        }

        public Code getCode() {
            return code;
        }
    }
}
