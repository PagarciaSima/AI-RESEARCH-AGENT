package com.pgs.ai.research.agent.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;

public interface ScrapedPostRepository extends JpaRepository<ScrapedPost, Long> {

	boolean existsByPlatformAndExternalId(Platform platform, String externalId);

	Object countByPlatform(Platform reddit);

	List<ScrapedPost> findByScrapedAtAfterOrderByScoreDesc(LocalDateTime since);

	List<ScrapedPost> findByPlatformOrderByScrapedAtDesc(Platform platform);

	List<ScrapedPost> findTop200ByOrderByScrapedAtDesc();
}
