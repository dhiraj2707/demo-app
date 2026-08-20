package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        return "Hello from the local CI/CD pipeline!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    public int add(int a, int b) {
        return a + b;
    }
}
