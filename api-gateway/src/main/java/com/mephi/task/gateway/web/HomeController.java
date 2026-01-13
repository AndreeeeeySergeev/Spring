package com.mephi.task.gateway.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class HomeController {

    @GetMapping({"/", "/ui"})
    public Mono<ResponseEntity<String>> index() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            byte[] bytes = resource.getInputStream().readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><h1>API Gateway</h1><p>Static resources not available</p></body></html>"));
        }
    }
}


