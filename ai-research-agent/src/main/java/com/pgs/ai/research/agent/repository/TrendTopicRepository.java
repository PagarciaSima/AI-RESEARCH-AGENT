package com.pgs.ai.research.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.TrendTopic;

public interface TrendTopicRepository extends JpaRepository<TrendTopic, Long> {
}
