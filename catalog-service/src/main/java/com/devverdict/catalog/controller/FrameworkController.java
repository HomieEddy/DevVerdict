package com.devverdict.catalog.controller;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.repository.FrameworkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/frameworks")
public class FrameworkController {

    private final FrameworkRepository frameworkRepository;

    public FrameworkController(FrameworkRepository frameworkRepository) {
        this.frameworkRepository = frameworkRepository;
    }

    @GetMapping
    public List<Framework> getAllFrameworks() {
        return frameworkRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Framework> getFrameworkById(@PathVariable Long id) {
        return frameworkRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
