package com.tracker.githubtracker.dto;

import com.tracker.githubtracker.entity.TrackedRepo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackedRepoResponseDto {

    private Long id;
    private String username;
    private String repoName;
    private int stars;
    private String language;
    private LocalDateTime lastUpdated;
    private LocalDateTime trackedSince;

    public static TrackedRepoResponseDto fromEntity(TrackedRepo entity) {
        if (entity == null) {
            return null;
        }
        return TrackedRepoResponseDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .repoName(entity.getRepoName())
                .stars(entity.getStars())
                .language(entity.getLanguage())
                .lastUpdated(entity.getLastUpdated())
                .trackedSince(entity.getTrackedSince())
                .build();
    }
}
