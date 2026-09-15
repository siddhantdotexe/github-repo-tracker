package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDto {

    private String name;

    @JsonProperty("stargazers_count")
    private int stargazersCount;

    private String language;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
