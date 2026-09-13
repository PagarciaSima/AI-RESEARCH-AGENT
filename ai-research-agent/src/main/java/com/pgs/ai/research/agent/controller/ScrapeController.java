package com.pgs.ai.research.agent.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;
import com.pgs.ai.research.agent.model.TrendAnalysis;
import com.pgs.ai.research.agent.repository.ScrapedPostRepository;
import com.pgs.ai.research.agent.service.LlmAnalysisService;
import com.pgs.ai.research.agent.service.ScrapingOrchestrator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scrape")
@RequiredArgsConstructor
public class ScrapeController {

	private final ScrapingOrchestrator orchestrator;
	private final LlmAnalysisService analysisService;
	private final ScrapedPostRepository postRepository;

	@PostMapping("/run")
	public ResponseEntity<Map<String, Object>> triggerFullCycle() {
		final Map<Platform, Integer> scrapeResults = this.orchestrator.scrapeAll();

		final LocalDateTime since = LocalDateTime.now().minusHours(6);
		final List<ScrapedPost> posts = this.postRepository.findByScrapedAtAfterOrderByScoreDesc(since);
		TrendAnalysis analysis = null;
		if (!posts.isEmpty()) {
			analysis = this.analysisService.analyze(posts);
		}

		return ResponseEntity.ok(Map.of("scrapeResults", scrapeResults, "postsAnalyzed", posts.size(), "analysisId",
				analysis != null ? analysis.getId() : "none"));
	}

	@PostMapping("/platform/{platform}")
	public ResponseEntity<List<ScrapedPost>> scrapePlatform(@PathVariable final Platform platform) {
		return ResponseEntity.ok(this.orchestrator.scrapePlatform(platform));
	}
	
	@GetMapping("/posts")
    public ResponseEntity<List<ScrapedPost>> getRecentPosts(
            @RequestParam(required = false)
            final Platform platform
    ) {
        if (platform != null) {
            return ResponseEntity.ok(this.postRepository.findByPlatformOrderByScrapedAtDesc(platform));
        }
        return ResponseEntity.ok(this.postRepository.findTop200ByOrderByScrapedAtDesc());
    }

}