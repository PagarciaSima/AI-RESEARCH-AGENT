package com.pgs.ai.research.agent.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.TrendTopic;

public interface TrendTopicRepository extends JpaRepository<TrendTopic, Long> {
    List<TrendTopic> findByDetectedAtAfterOrderByTrendScoreDesc(LocalDateTime since);

	List<TrendTopic> findTop20ByOrderByTrendScoreDesc();

	List<TrendTopic> findByCategoryOrderByTrendScoreDesc(String category);

	List<TrendTopic> findByPrimaryPlatformOrderByTrendScoreDesc(String platform);

}