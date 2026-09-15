package com.tracker.githubtracker.repository;

import com.tracker.githubtracker.entity.TrackedRepo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedRepoRepository extends JpaRepository<TrackedRepo, Long> {

    List<TrackedRepo> findByUsername(String username);

    List<TrackedRepo> findByLanguageIgnoreCase(String language);

    List<TrackedRepo> findByLanguageIgnoreCase(String language, Sort sort);

    Optional<TrackedRepo> findByUsernameAndRepoName(String username, String repoName);

    void deleteByUsername(String username);

    long countByUsername(String username);

    @Query("SELECT DISTINCT r.username FROM TrackedRepo r")
    List<String> findDistinctUsernames();
}
