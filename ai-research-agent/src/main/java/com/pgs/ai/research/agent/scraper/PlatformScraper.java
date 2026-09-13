package com.pgs.ai.research.agent.scraper;

import java.util.List;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;

public interface PlatformScraper {

    Platform getPlatform();

    List<ScrapedPost> scrape();
}
