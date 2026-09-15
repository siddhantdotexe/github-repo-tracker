package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.GitHubRepoDto;
import com.tracker.githubtracker.dto.TrackedRepoResponseDto;
import com.tracker.githubtracker.entity.TrackedRepo;
import com.tracker.githubtracker.repository.TrackedRepoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RepoTrackingService {
    
    private static final Logger log = LoggerFactory.getLogger(RepoTrackingService.class);

    private final GitHubClient gitHubClient;
    private final TrackedRepoRepository trackedRepoRepository;

    public RepoTrackingService(GitHubClient gitHubClient, TrackedRepoRepository trackedRepoRepository) {
        this.gitHubClient = gitHubClient;
        this.trackedRepoRepository = trackedRepoRepository;
    }

    @Transactional
    public List<TrackedRepoResponseDto> trackUserRepos(String username) {
        log.info("Tracking repos for user: {}", username);
        List<GitHubRepoDto> githubRepos = gitHubClient.fetchUserRepos(username);

        List<TrackedRepo> updatedRepos = githubRepos.stream().map(dto -> {
            Optional<TrackedRepo> existingOpt = trackedRepoRepository.findByUsernameAndRepoName(username, dto.getName());
            
            LocalDateTime dtoUpdated = dto.getUpdatedAt() != null ? dto.getUpdatedAt().toLocalDateTime() : LocalDateTime.now();
            
            TrackedRepo repo;
            if (existingOpt.isPresent()) {
                repo = existingOpt.get();
                repo.setStars(dto.getStargazersCount());
                repo.setLanguage(dto.getLanguage());
                repo.setLastUpdated(dtoUpdated);
            } else {
                repo = TrackedRepo.builder()
                        .username(username)
                        .repoName(dto.getName())
                        .stars(dto.getStargazersCount())
                        .language(dto.getLanguage())
                        .lastUpdated(dtoUpdated)
                        .build();
            }
            return repo;
        }).collect(Collectors.toList());

        List<TrackedRepo> savedRepos = trackedRepoRepository.saveAll(updatedRepos);
        return savedRepos.stream().map(TrackedRepoResponseDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrackedRepoResponseDto> getTrackedRepos(String language, String sortBy) {
        Sort sort = Sort.unsorted();
        if ("stars".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "stars");
        } else if ("lastUpdated".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "lastUpdated");
        }

        List<TrackedRepo> repos;
        if (language != null && !language.isBlank()) {
            repos = sort.isUnsorted() ? 
                    trackedRepoRepository.findByLanguageIgnoreCase(language) : 
                    trackedRepoRepository.findByLanguageIgnoreCase(language, sort);
        } else {
            repos = sort.isUnsorted() ? 
                    trackedRepoRepository.findAll() : 
                    trackedRepoRepository.findAll(sort);
        }

        return repos.stream().map(TrackedRepoResponseDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrackedRepoResponseDto> getTrackedReposByUsername(String username) {
        return trackedRepoRepository.findByUsername(username).stream()
                .map(TrackedRepoResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReposByUsername(String username) {
        long count = trackedRepoRepository.countByUsername(username);
        log.info("Deleting {} repos for user: {}", count, username);
        trackedRepoRepository.deleteByUsername(username);
    }

    @Scheduled(fixedRate = 86400000) // 24 hours
    @Transactional
    public void refreshAllTrackedRepos() {
        log.info("Starting daily refresh of all tracked repos...");
        List<String> usernames = trackedRepoRepository.findDistinctUsernames();
        int totalRefreshed = 0;

        for (String username : usernames) {
            try {
                List<TrackedRepoResponseDto> updated = trackUserRepos(username);
                totalRefreshed += updated.size();
                log.info("Refreshed {} repos for user: {}", updated.size(), username);
            } catch (Exception e) {
                log.error("Failed to refresh repos for user: {}. Error: {}", username, e.getMessage());
            }
        }
        
        log.info("Completed daily refresh. Total repos refreshed: {}", totalRefreshed);
    }
}
