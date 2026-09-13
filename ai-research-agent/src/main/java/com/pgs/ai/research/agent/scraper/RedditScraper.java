package com.pgs.ai.research.agent.scraper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pgs.ai.research.agent.config.ProxyConfig;
import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;
import com.pgs.ai.research.agent.repository.ScrapedPostRepository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class RedditScraper extends AbstractScraper implements PlatformScraper {

	private final ScrapedPostRepository postRepository;
	private final ObjectMapper objectMapper;

	public RedditScraper(final ProxyConfig proxyConfig, final ScrapedPostRepository postRepository,
			final ObjectMapper objectMapper) {
		super(proxyConfig);
		this.postRepository = postRepository;
		this.objectMapper = objectMapper;
	}

	@Value("${scraping.reddit.subreddits}")
	private List<String> subreddits;

	@Value("${scraping.reddit.posts-per-subreddit}")
	private int postsPerSubreddit;

	@Override
    public Platform getPlatform() {
        return Platform.REDDIT;
    }

	@Override
    public List<ScrapedPost> scrape() {
        final List<ScrapedPost> posts = new ArrayList<>();

        log.info("Reddit scraper started");

        log.info("Reddit scraper using subreddits: {}", this.subreddits);

        for (final String subreddit:this.subreddits){
    		try {
    			final String url = "https://www.reddit.com/r/" + subreddit + "/hot.json?limit=" + this.postsPerSubreddit;

    			final String json = fetch(url);

    			final String proxyIp = detectProxyIp();

    			final JsonNode root = this.objectMapper.readTree(json);
    			final JsonNode children = root.path("data").path("children");

    			for (final JsonNode child : children) {
    				final JsonNode data = child.path("data");
    				final String externalId = data.path("id").asString("");

    				if (externalId.isBlank()) {
    					continue;
    				}
    				if (this.postRepository.existsByPlatformAndExternalId(getPlatform(), externalId)) {
    					continue;
    				}
    				
    				final String title = data.path("title")
                            .asString();
    				
    				if (title.isBlank()) {
                        continue;
                    }

                    final String selftext = data.path("selftext")
                            .asString();
                    
                    final String content = selftext.isBlank()
                            ? title.substring(0, Math.min(title.length(), 500))
                            : selftext;
                    
                    final long postedAtEpoch = (long) data.path("created_utc")
                            .asDouble();

                    final LocalDateTime postedAt = data.has("created_utc")
                            ? LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(postedAtEpoch),
                                    ZoneId.systemDefault()
                            )
                            : null;
                    
                    final String redditUrl = data.path("url").asString(null);
                    final String author = data.path("author").asString(null);
                    final int score = data.path("score").asInt(0);
                    final int commentCount = data.path("num_comments").asInt(0);
                    final String subredditName = data.path("subreddit").asString(subreddit);
                    
                    final ScrapedPost post = ScrapedPost.builder()
                            .platform(getPlatform())
                            .externalId(externalId)
                            .title(title)
                            .content(content)
                            .proxyIpUsed(proxyIp)
                            .url(redditUrl)
                            .author(author)
                            .score(score)
                            .commentCount(commentCount)
                            .subReddit(subredditName)
                            .postedAt(postedAt)
                            .build();
                    
                    posts.add(post);
    			}
    			log.info("Reddit r/{} scraped: {} new posts", subreddit, posts.size());
    			Thread.sleep(500);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                log.error("Failed to scrape Reddit r/{}", subreddit, e.getMessage());
            }
    	}
        return posts;
    }

	
}