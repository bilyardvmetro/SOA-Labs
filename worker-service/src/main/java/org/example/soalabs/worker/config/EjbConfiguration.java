package org.example.soalabs.worker.config;

import org.example.soalabs.worker.service.WorkerRemote;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

@Configuration
public class EjbConfiguration {
    @Bean(destroyMethod = "close")
    Context workerEjbContext(Environment env) throws NamingException {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        properties.put(Context.PROVIDER_URL, env.getProperty("WORKER_EJB_URL", "remote+http://127.0.0.1:19080"));
        properties.put(Context.SECURITY_PRINCIPAL, env.getRequiredProperty("WORKER_EJB_USERNAME"));
        properties.put(Context.SECURITY_CREDENTIALS, env.getRequiredProperty("WORKER_EJB_PASSWORD"));
        properties.put("jboss.naming.client.ejb.context", true);
        return new InitialContext(properties);
    }

    @Bean
    WorkerRemote workerRemote(Context workerEjbContext) throws NamingException {
        return (WorkerRemote) workerEjbContext.lookup(
                "worker/worker-ejb/WorkerService!org.example.soalabs.worker.service.WorkerRemote");
    }
}
