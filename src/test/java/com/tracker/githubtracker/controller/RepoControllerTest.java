package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.TrackedRepoResponseDto;
import com.tracker.githubtracker.exception.RateLimitExceededException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import com.tracker.githubtracker.service.RepoTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepoController.class)
class RepoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepoTrackingService repoTrackingService;

    @Test
    void testTrackRepos_Success() throws Exception {
        TrackedRepoResponseDto dto = new TrackedRepoResponseDto();
        dto.setUsername("testuser");
        dto.setRepoName("repo1");
        dto.setLanguage("Java");
        dto.setStars(100);
        dto.setLastUpdated(LocalDateTime.of(2023, 1, 1, 12, 0));

        when(repoTrackingService.trackUserRepos("testuser")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(post("/api/repos/track/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].repoName").value("repo1"))
                .andExpect(jsonPath("$[0].language").value("Java"))
                .andExpect(jsonPath("$[0].stars").value(100));

        verify(repoTrackingService).trackUserRepos("testuser");
    }

    @Test
    void testGetAllTrackedRepos_WithParams() throws Exception {
        TrackedRepoResponseDto dto = new TrackedRepoResponseDto();
        dto.setUsername("user1");
        dto.setRepoName("repo1");

        when(repoTrackingService.getTrackedRepos("Java", "stars")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/repos")
                        .param("language", "Java")
                        .param("sortBy", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"));

        verify(repoTrackingService).getTrackedRepos("Java", "stars");
    }

    @Test
    void testGetReposByUsername_Success() throws Exception {
        TrackedRepoResponseDto dto = new TrackedRepoResponseDto();
        dto.setUsername("testuser");
        dto.setRepoName("repo1");

        when(repoTrackingService.getTrackedReposByUsername("testuser")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/repos/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repoName").value("repo1"));

        verify(repoTrackingService).getTrackedReposByUsername("testuser");
    }

    @Test
    void testGetReposByUsername_UserNotFound_ErrorHandling() throws Exception {
        when(repoTrackingService.getTrackedReposByUsername("unknown")).thenThrow(new UserNotFoundException("GitHub user not found: unknown"));

        mockMvc.perform(get("/api/repos/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("GitHub user not found: unknown"))
                .andExpect(jsonPath("$.path").value("/api/repos/unknown"));
    }

    @Test
    void testDeleteReposByUsername() throws Exception {
        doNothing().when(repoTrackingService).deleteReposByUsername("testuser");

        mockMvc.perform(delete("/api/repos/testuser"))
                .andExpect(status().isNoContent());

        verify(repoTrackingService).deleteReposByUsername("testuser");
    }
}
