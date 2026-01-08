package com.gianniniseba.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")

public class AdminController {

    @GetMapping("/ping")
    public Map<String, String> adminPing(){
        return Map.of("message","Hola Admin, todo piola");
    }

}
