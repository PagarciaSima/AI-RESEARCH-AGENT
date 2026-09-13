package com.pgs.ai.research.agent.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;
import com.pgs.ai.research.agent.repository.ScrapedPostRepository;
import com.pgs.ai.research.agent.scraper.PlatformScraper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingOrchestrator {

	private final List<PlatformScraper> scrapers;
	private final ScrapedPostRepository postRepository;

	public Map<Platform, Integer> scrapeAll() {
		final Map<Platform, Integer> results = new EnumMap<>(Platform.class);

		for (final PlatformScraper scraper : scrapers) {
			try {
				final List<ScrapedPost> scrapedPosts = scraper.scrape();
				final List<ScrapedPost> saved = this.postRepository.saveAll(scrapedPosts);
				results.put(scraper.getPlatform(), saved.size());
				log.info("Scraped {} new posts from {}", saved.size(), scraper.getPlatform());
			} catch (final Exception e) {
				log.error("Failed to scrape {}", scraper.getPlatform(), e.getMessage());
				results.put(scraper.getPlatform(), 0);
			}
		}

		return results;
	}
	
	public List<ScrapedPost> scrapePlatform(final Platform platform) {
        return this.scrapers.stream()
                .filter(scraper -> scraper.getPlatform() == platform)
                .findFirst()
                .map(PlatformScraper::scrape)
                .map(this.postRepository::saveAll)
                .orElse(List.of());
    }
}
