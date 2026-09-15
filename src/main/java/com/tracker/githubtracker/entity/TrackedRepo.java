package com.tracker.githubtracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracked_repos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_username_reponame", columnNames = {"username", "repoName"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackedRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String repoName;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = true)
    private String language;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column(nullable = false, updatable = false)
    private LocalDateTime trackedSince;

    @PrePersist
    protected void onCreate() {
        if (this.trackedSince == null) {
            this.trackedSince = LocalDateTime.now();
        }
    }
}
