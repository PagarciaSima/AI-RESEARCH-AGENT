package com.pgs.ai.research.agent.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;
import com.pgs.ai.research.agent.repository.ScrapedPostRepository;
import com.pgs.ai.research.agent.service.LlmAnalysisService;
import com.pgs.ai.research.agent.service.ScrapingOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResearchScheduler {

    private final ScrapingOrchestrator scrapingOrchestrator;
    private final ScrapedPostRepository postRepository;
    private final LlmAnalysisService analysisService;
    
    @Scheduled(cron = "${scraping.cron}")
    public void runResearchCycle() {

        log.info("====== Research cycle started ======");

        final Map<Platform, Integer> results = this.scrapingOrchestrator.scrapeAll();

        log.info("Scraping completed. Results: {}", results);

        final LocalDateTime since = LocalDateTime.now().minusHours(6);

        final List<ScrapedPost> recentPosts = this.postRepository.findByScrapedAtAfterOrderByScoreDesc(since);
        
        if (!recentPosts.isEmpty()) {
            this.analysisService.analyze(recentPosts);
            log.info("LLM analysis completed for {} posts.", recentPosts.size());
        }
        
        log.info("====== Research cycle completed ======");
    }
}
