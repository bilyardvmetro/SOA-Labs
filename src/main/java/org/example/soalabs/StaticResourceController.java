package org.example.soalabs;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class StaticResourceController {

    @GetMapping("/openapi/worker-service")
    public ResponseEntity<Resource> getWorkerSpec() throws IOException {
        Resource res = new ClassPathResource("static/openapi/worker-service.yaml");
        if (!res.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-yaml"));
        return new ResponseEntity<>(res, headers, HttpStatus.OK);
    }

    @GetMapping("/openapi/worker-service.yaml")
    public ResponseEntity<Resource> getWorkerSpecYaml() throws IOException {
        return getWorkerSpec();
    }

    @GetMapping("/openapi/hr-service")
    public ResponseEntity<Resource> getHrSpec() throws IOException {
        Resource res = new ClassPathResource("static/openapi/hr-service.yaml");
        if (!res.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-yaml"));
        return new ResponseEntity<>(res, headers, HttpStatus.OK);
    }

    @GetMapping("/openapi/hr-service.yaml")
    public ResponseEntity<Resource> getHrSpecYaml() throws IOException {
        return getHrSpec();
    }

    @GetMapping("/docs")
    public ResponseEntity<Resource> getDocsIndex() throws IOException {
        Resource res = new ClassPathResource("static/docs/index.html");
        if (!res.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(res, headers, HttpStatus.OK);
    }

    @GetMapping("/docs/index.html")
    public ResponseEntity<Resource> getDocsIndexHtml() throws IOException {
        return getDocsIndex();
    }
}
