package com.pgs.ai.research.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.TrendAnalysis;

public interface TrendAnalysisRepository extends JpaRepository<TrendAnalysis, Long> {
}
