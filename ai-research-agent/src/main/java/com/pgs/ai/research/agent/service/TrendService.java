package com.pgs.ai.research.agent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.TrendAnalysis;
import com.pgs.ai.research.agent.model.TrendAnalysis;
import com.pgs.ai.research.agent.model.TrendTopic;
import com.pgs.ai.research.agent.repository.ScrapedPostRepository;
import com.pgs.ai.research.agent.repository.TrendAnalysisRepository;
import com.pgs.ai.research.agent.repository.TrendTopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {

    private final TrendTopicRepository trendTopicRepository;
    private final ScrapedPostRepository scrapedPostRepository;
    private final TrendAnalysisRepository trendAnalysisRepository;

    public List<TrendTopic> getLatestTrends() {
        final LocalDateTime since = LocalDateTime.now().minusHours(24);
        return this.trendTopicRepository.findByDetectedAtAfterOrderByTrendScoreDesc(since);
    }
    
    public List<TrendTopic> getTopTrends() {
        return this.trendTopicRepository.findTop20ByOrderByTrendScoreDesc();
    }

    public List<TrendTopic> getTrendsByCategory(final String category) {
        return this.trendTopicRepository.findByCategoryOrderByTrendScoreDesc(category);
    }
    
    public List<TrendTopic> getTrendsByPlatform(final String platform) {
        return this.trendTopicRepository.findByPrimaryPlatformOrderByTrendScoreDesc(platform);
    }
    
    public Map<String, Object> getDashboardStats() {
        return Map.of(
                "totalPosts", this.scrapedPostRepository.count(),
                "redditPosts", this.scrapedPostRepository.countByPlatform(Platform.REDDIT),
                "hnPosts", this.scrapedPostRepository.countByPlatform(Platform.HACKERNEWS),
                "phPosts", this.scrapedPostRepository.countByPlatform(Platform.PRODUCTHUNT),
                "totalTrends", this.trendTopicRepository.count(),
                "lastAnalysis", this.trendAnalysisRepository.findTopByOrderByAnalyzedAtDesc()
                        .map(TrendAnalysis::getAnalyzedAt)
                        .orElse(LocalDateTime.now())
        );
    }
}