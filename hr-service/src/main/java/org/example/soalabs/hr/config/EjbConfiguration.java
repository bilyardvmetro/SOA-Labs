package org.example.soalabs.hr.config;

import org.example.soalabs.hr.service.HrRemote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Configuration
public class EjbConfiguration {
    @Bean
    HrRemote hrRemote() throws NamingException {
        InitialContext context = new InitialContext();
        try {
            return (HrRemote) context.lookup("java:global/hr/hr-ejb/HrService!org.example.soalabs.hr.service.HrRemote");
        } finally {
            context.close();
        }
    }
}
