package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.GitHubRepoDto;
import com.tracker.githubtracker.dto.TrackedRepoResponseDto;
import com.tracker.githubtracker.entity.TrackedRepo;
import com.tracker.githubtracker.exception.RateLimitExceededException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import com.tracker.githubtracker.repository.TrackedRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepoTrackingServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private TrackedRepoRepository trackedRepoRepository;

    @InjectMocks
    private RepoTrackingService repoTrackingService;

    private GitHubRepoDto repoDto1;
    private GitHubRepoDto repoDto2;

    @BeforeEach
    void setUp() {
        repoDto1 = new GitHubRepoDto();
        repoDto1.setName("repo1");
        repoDto1.setStargazersCount(100);
        repoDto1.setLanguage("Java");
        repoDto1.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        repoDto2 = new GitHubRepoDto();
        repoDto2.setName("repo2");
        repoDto2.setStargazersCount(50);
        repoDto2.setLanguage("Python");
        repoDto2.setUpdatedAt(OffsetDateTime.now().minusDays(2));
    }

    @Test
    void testTrackUserRepos_Success_InsertAndUpdate() {
        String username = "testuser";
        when(gitHubClient.fetchUserRepos(username)).thenReturn(Arrays.asList(repoDto1, repoDto2));

        // repo1 is new (Insert)
        when(trackedRepoRepository.findByUsernameAndRepoName(username, "repo1")).thenReturn(Optional.empty());

        // repo2 already exists (Update)
        TrackedRepo existingRepo2 = new TrackedRepo();
        existingRepo2.setId(2L);
        existingRepo2.setUsername(username);
        existingRepo2.setRepoName("repo2");
        existingRepo2.setStars(10);
        when(trackedRepoRepository.findByUsernameAndRepoName(username, "repo2")).thenReturn(Optional.of(existingRepo2));

        // Mock saveAll
        when(trackedRepoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TrackedRepoResponseDto> result = repoTrackingService.trackUserRepos(username);

        assertEquals(2, result.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrackedRepo>> captor = ArgumentCaptor.forClass(List.class);
        verify(trackedRepoRepository).saveAll(captor.capture());
        
        List<TrackedRepo> savedRepos = captor.getValue();
        assertEquals(2, savedRepos.size());
        
        TrackedRepo savedRepo1 = savedRepos.get(0);
        assertEquals("repo1", savedRepo1.getRepoName());
        assertEquals(100, savedRepo1.getStars());
        assertNull(savedRepo1.getId()); // It was a new instance

        TrackedRepo savedRepo2 = savedRepos.get(1);
        assertEquals("repo2", savedRepo2.getRepoName());
        assertEquals(50, savedRepo2.getStars()); // Updated stars
        assertEquals(2L, savedRepo2.getId()); // Existing instance updated
    }

    @Test
    void testTrackUserRepos_UserNotFound() {
        String username = "unknownuser";
        when(gitHubClient.fetchUserRepos(username)).thenThrow(new UserNotFoundException("GitHub user not found: " + username));

        assertThrows(UserNotFoundException.class, () -> repoTrackingService.trackUserRepos(username));
        verify(trackedRepoRepository, never()).saveAll(any());
    }

    @Test
    void testTrackUserRepos_RateLimitExceeded() {
        String username = "testuser";
        when(gitHubClient.fetchUserRepos(username)).thenThrow(new RateLimitExceededException("GitHub API rate limit exceeded"));

        assertThrows(RateLimitExceededException.class, () -> repoTrackingService.trackUserRepos(username));
        verify(trackedRepoRepository, never()).saveAll(any());
    }
    
    @Test
    void testGetTrackedRepos_WithLanguageAndSort() {
        TrackedRepo repo = new TrackedRepo();
        repo.setId(1L);
        repo.setUsername("testuser");
        repo.setRepoName("repo1");
        repo.setLanguage("Java");
        
        when(trackedRepoRepository.findByLanguageIgnoreCase(eq("Java"), any(Sort.class)))
                .thenReturn(Collections.singletonList(repo));

        List<TrackedRepoResponseDto> result = repoTrackingService.getTrackedRepos("Java", "stars");
        
        assertEquals(1, result.size());
        assertEquals("repo1", result.get(0).getRepoName());
        
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(trackedRepoRepository).findByLanguageIgnoreCase(eq("Java"), sortCaptor.capture());
        Sort sort = sortCaptor.getValue();
        assertTrue(sort.getOrderFor("stars") != null);
        assertTrue(sort.getOrderFor("stars").isDescending());
    }
    
    @Test
    void testGetTrackedRepos_NoParams() {
        TrackedRepo repo = new TrackedRepo();
        repo.setId(1L);
        repo.setUsername("testuser");
        repo.setRepoName("repo1");
        
        when(trackedRepoRepository.findAll()).thenReturn(Collections.singletonList(repo));

        List<TrackedRepoResponseDto> result = repoTrackingService.getTrackedRepos(null, null);
        
        assertEquals(1, result.size());
        verify(trackedRepoRepository).findAll();
    }
    
    @Test
    void testGetTrackedReposByUsername() {
        TrackedRepo repo = new TrackedRepo();
        repo.setId(1L);
        repo.setUsername("testuser");
        repo.setRepoName("repo1");
        
        when(trackedRepoRepository.findByUsername("testuser")).thenReturn(Collections.singletonList(repo));
        
        List<TrackedRepoResponseDto> result = repoTrackingService.getTrackedReposByUsername("testuser");
        
        assertEquals(1, result.size());
        verify(trackedRepoRepository).findByUsername("testuser");
    }
    
    @Test
    void testDeleteReposByUsername() {
        when(trackedRepoRepository.countByUsername("testuser")).thenReturn(5L);
        
        repoTrackingService.deleteReposByUsername("testuser");
        
        verify(trackedRepoRepository).deleteByUsername("testuser");
    }
    
    @Test
    void testRefreshAllTrackedRepos() {
        when(trackedRepoRepository.findDistinctUsernames()).thenReturn(Arrays.asList("user1", "user2"));
        
        // Mock success for user1
        when(gitHubClient.fetchUserRepos("user1")).thenReturn(Collections.singletonList(repoDto1));
        when(trackedRepoRepository.findByUsernameAndRepoName("user1", "repo1")).thenReturn(Optional.empty());
        when(trackedRepoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock exception for user2 to ensure loop continues
        when(gitHubClient.fetchUserRepos("user2")).thenThrow(new RateLimitExceededException("Limit"));
        
        repoTrackingService.refreshAllTrackedRepos();
        
        verify(gitHubClient).fetchUserRepos("user1");
        verify(gitHubClient).fetchUserRepos("user2");
        verify(trackedRepoRepository, times(1)).saveAll(any());
    }
}
