package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.TrackedRepoResponseDto;
import com.tracker.githubtracker.service.RepoTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoTrackingService repoTrackingService;

    public RepoController(RepoTrackingService repoTrackingService) {
        this.repoTrackingService = repoTrackingService;
    }

    @PostMapping("/track/{username}")
    public ResponseEntity<List<TrackedRepoResponseDto>> trackRepos(@PathVariable String username) {
        List<TrackedRepoResponseDto> trackedRepos = repoTrackingService.trackUserRepos(username);
        return ResponseEntity.ok(trackedRepos);
    }

    @GetMapping
    public ResponseEntity<List<TrackedRepoResponseDto>> getAllTrackedRepos(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String sortBy) {
        
        List<TrackedRepoResponseDto> repos = repoTrackingService.getTrackedRepos(language, sortBy);
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/{username}")
    public ResponseEntity<List<TrackedRepoResponseDto>> getReposByUsername(@PathVariable String username) {
        List<TrackedRepoResponseDto> repos = repoTrackingService.getTrackedReposByUsername(username);
        return ResponseEntity.ok(repos);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteReposByUsername(@PathVariable String username) {
        repoTrackingService.deleteReposByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
