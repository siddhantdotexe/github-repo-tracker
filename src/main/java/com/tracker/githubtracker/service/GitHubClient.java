package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.GitHubRepoDto;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.exception.RateLimitExceededException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    
    private final RestTemplate restTemplate;
    private final String githubApiBaseUrl;

    public GitHubClient(RestTemplate restTemplate, @Value("${github.api.base-url}") String githubApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.githubApiBaseUrl = githubApiBaseUrl;
    }

    public List<GitHubRepoDto> fetchUserRepos(String username) {
        String url = String.format("%s/users/%s/repos", githubApiBaseUrl, username);
        log.info("Fetching repos from GitHub for user: {}", username);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "github-repo-tracker-app");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            String rawJson = response.getBody();
            log.info("EXACT URL CALLED: {}", url);
            log.info("RAW HTTP STATUS CODE: {}", response.getStatusCode());
            if (rawJson != null) {
                log.info("RAW JSON RESPONSE (first 2000 chars): \n{}", rawJson.substring(0, Math.min(rawJson.length(), 2000)));
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            
            return mapper.readValue(rawJson, new com.fasterxml.jackson.core.type.TypeReference<List<GitHubRepoDto>>() {});
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new UserNotFoundException("GitHub user not found: " + username);
            } else if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 429) {
                throw new RateLimitExceededException("GitHub API rate limit exceeded");
            }
            log.error("HTTP error while fetching GitHub repos: {}", e.getMessage());
            throw new GitHubApiException("Failed to fetch repos for user " + username + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Network or parsing error while fetching GitHub repos: {}", e.getMessage());
            throw new GitHubApiException("Failed to communicate with GitHub API: " + e.getMessage());
        }
    }
}
