package com.pgs.ai.research.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.ScrapedPost;

public interface ScrapedPostRepository extends JpaRepository<ScrapedPost, Long> {
}
