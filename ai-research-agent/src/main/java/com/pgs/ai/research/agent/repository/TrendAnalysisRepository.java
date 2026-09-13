package com.pgs.ai.research.agent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pgs.ai.research.agent.model.TrendAnalysis;

public interface TrendAnalysisRepository extends JpaRepository<TrendAnalysis, Long> {

	Optional<TrendAnalysis> findTopByOrderByAnalyzedAtDesc();
}
