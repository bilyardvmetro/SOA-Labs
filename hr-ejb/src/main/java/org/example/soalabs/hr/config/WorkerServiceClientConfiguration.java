package org.example.soalabs.hr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.validation.Validator;
import org.example.soalabs.hr.client.WorkerServiceClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Singleton
@Startup
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class WorkerServiceClientConfiguration {
    @Resource
    private Validator validator;
    private ObjectMapper mapper;
    private RestClient.Builder builder;
    private URI esbEndpoint;

    @PostConstruct
    void initialize() {
        WorkerServiceProperties properties = WorkerServiceProperties.environment();

        mapper = JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        builder = RestClient.builder().requestFactory(factory)
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(mapper));
                });
        esbEndpoint = properties.esbUrl();
    }

    public WorkerServiceClient client() {
        return new WorkerServiceClient(builder.clone().baseUrl(esbEndpoint.toString()).build(), mapper, validator);
    }

    public void health() {
        if (esbEndpoint == null || validator == null) throw new IllegalStateException("HR EJB is not ready");
    }
}
