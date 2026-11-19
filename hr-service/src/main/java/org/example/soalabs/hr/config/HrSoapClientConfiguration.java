package org.example.soalabs.hr.config;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.example.soalabs.hr.soap.HrPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

@Configuration
public class HrSoapClientConfiguration {
    @Bean(destroyMethod = "close")
    HrPort hrSoapClient(@Value("${hr.soap.endpoint}") String endpoint) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(endpoint);
        factory.setBindingId(SOAPBinding.SOAP11HTTP_BINDING);
        factory.setServiceName(new QName(HrPort.NAMESPACE, "HrService"));
        factory.setEndpointName(new QName(HrPort.NAMESPACE, "HrPort"));

        HrPort port = factory.create(HrPort.class);
        ((BindingProvider) port).getRequestContext().put("thread.local.request.context", "true");

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(3000);
        policy.setReceiveTimeout(20000);
        HTTPConduit conduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        conduit.setClient(policy);
        return port;
    }
}
