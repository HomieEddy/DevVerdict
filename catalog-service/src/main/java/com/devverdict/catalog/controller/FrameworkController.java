package com.devverdict.catalog.controller;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.dto.FrameworkRequest;
import com.devverdict.catalog.repository.FrameworkRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

    @GetMapping("/search")
    public List<Framework> searchFrameworks(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minRating) {
        return frameworkRepository.searchFrameworks(name, type, minRating);
    }

    @PostMapping
    public ResponseEntity<Framework> createFramework(
            @Valid @RequestBody FrameworkRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Framework framework = new Framework(
                request.getName(),
                request.getType(),
                request.getDescription(),
                request.getAverageRating()
        );
        Framework saved = frameworkRepository.save(framework);
        return ResponseEntity.created(URI.create("/api/catalog/frameworks/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Framework> updateFramework(
            @PathVariable Long id,
            @Valid @RequestBody FrameworkRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return frameworkRepository.findById(id)
                .map(framework -> {
                    framework.setName(request.getName());
                    framework.setType(request.getType());
                    framework.setDescription(request.getDescription());
                    framework.setAverageRating(request.getAverageRating());
                    Framework updated = frameworkRepository.save(framework);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFramework(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return frameworkRepository.findById(id)
                .map(framework -> {
                    if (framework.getReviewCount() > 0) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
                    }
                    frameworkRepository.delete(framework);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }
}
