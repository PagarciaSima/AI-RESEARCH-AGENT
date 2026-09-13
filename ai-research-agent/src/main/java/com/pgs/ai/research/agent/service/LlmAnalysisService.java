package com.pgs.ai.research.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pgs.ai.research.agent.model.Platform;
import com.pgs.ai.research.agent.model.ScrapedPost;
import com.pgs.ai.research.agent.model.TrendAnalysis;
import com.pgs.ai.research.agent.model.TrendTopic;
import com.pgs.ai.research.agent.repository.TrendAnalysisRepository;
import com.pgs.ai.research.agent.repository.TrendTopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisService {

	private static final String SYSTEM_PROMPT = """
	        You are an AI research analyst specializing in technology trends and startup ideas.
	        
	        You will receive a batch of posts from Reddit, Hacker News, and Product Hunt.
	        
	        Analyze them and identify the top emerging trends, recurring topics, and notable signals.
	        
	        For each trend you identify, respond with ONLY a JSON array (no markdown, no preamble):
	        [
	            {
	                "topic": "Short topic name",
	                "summary": "2-3 sentence summary of the trend",
	                "reasoning": "Why this is significant and why it's trending now",
	                "category": "One of: AI/ML, DevTools, SaaS, Infrastructure, Security, Web3, Hardware, Other",
	                "mentionCount": 5,
	                "trendScore": 0.85,
	                "primaryPlatform": "REDDIT",
	                "relatedPostIds": ["ext_id_1", "ext_id_2"]
	            }
	        ]
	        
	        Rules:
	        - Identify 5-10 trends maximum
	        - trendScore is 0.0 to 1.0 (1.0 = extremely strong signal)
	        - mentionCount = how many posts in this batch relate to this trend
	        - Only include trends mentioned in 2+ posts OR with very high engagement (score > 200)
	        - Focus on EMERGING trends, not well-established topics
	        - primaryPlatform = where the trend is strongest
	        """;

    private final WebClient anthropicWebClient;
    private final TrendAnalysisRepository analysisRepository;
    private final TrendTopicRepository topicRepository;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.model}")
    private String model;

    public TrendAnalysis analyze(final List<ScrapedPost> posts) {
        final String userPrompt = buildPrompt(posts);
        String rawResponse = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
        	try {
        	    // Anthropic Messages API: system is a top-level field, not inside messages
        	    final Map<String, Object> requestBody = Map.of(
        	            "model", model,
        	            "max_tokens", 4000,
        	            "system", SYSTEM_PROMPT,
        	            "messages", List.of(Map.of("role", "user", "content", userPrompt))
        	    );
        	    rawResponse = this.anthropicWebClient.post()
        	            .uri("/messages")
        	            .bodyValue(requestBody)
        	            .retrieve()
        	            .bodyToMono(String.class)
        	            .block();
        	    break;
        	} catch (final Exception e) {
        	    log.warn("Anthropic attempt {}/3 failed: {}", attempt, e.getMessage());
        	    if (attempt < 3) {
        	        try {
        	            Thread.sleep(2000);
        	        } catch (final InterruptedException ie) {
        	            Thread.currentThread().interrupt();
        	        }
        	    }
        	}
        }
        if (rawResponse == null) {
    	    log.error("Anthropic attemps failed");
    	    rawResponse ="{}";
    	}
    	
    	TrendAnalysis analysis = TrendAnalysis.builder()
    	        .rawAnalysis(rawResponse)
    	        .postsAnalysis(posts.size())
    	        .build();

    	analysis = this.analysisRepository.save(analysis);
    	final List<TrendTopic> topics = parseTopics(rawResponse, analysis);
    	log.info("Saved {} trend topics from analysis of {} posts.", topics.size(), posts.size());

        return analysis;
    }
    
    private String buildPrompt(final List<ScrapedPost> posts) {
        final StringBuilder prompt = new StringBuilder("Here are the latest posts from tech communities:\n\n");
        for (final ScrapedPost post : posts) {
            prompt.append(String.format("[%s] (score: %d, comments: %d) %s%n",
                    post.getPlatform(),
                    post.getScore(),
                    post.getCommentCount(),
                    post.getTitle()
            ));
            if (post.getContent() != null && !post.getContent().isBlank()) {

                final String snippet = post.getContent().length() > 200
                        ? post.getContent().substring(0, 200) + "..."
                        : post.getContent();
                prompt.append(" > ").append(snippet).append("\n");

            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    private List<TrendTopic> parseTopics(final String rawReponse, final TrendAnalysis analysis) {
        final List<TrendTopic> topics = new ArrayList<>();
        try {
            final JsonNode root = this.objectMapper.readTree(rawReponse);
            
            final JsonNode contentArray = root.path("content");
            if (!contentArray.isArray() || contentArray.isEmpty()) {
                log.error("Unexpected Anthropic response structure: {}", rawReponse);
                return topics;
            }
            String content = contentArray.get(0).path("text").asString("").strip();
            
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```[a-zA-Z]*\\n", "");
                content = content.replaceFirst("```$\\n", "").strip();
            }
            
            final JsonNode trendsArray = this.objectMapper.readTree(content);
            if (!trendsArray.isArray()) {
                return topics;
            }

            for (final JsonNode node : trendsArray) {
                try {
                    final Platform platform = parsePlatform(node.path("primaryPlatform").asString(""));
                    final List<String> relatedIds = new ArrayList<>();
                    final JsonNode relatedNode = node.path("relatedPostIds");
                    if (relatedNode.isArray()) {
                        for (final JsonNode id : relatedNode) {
                            relatedIds.add(id.asString());
                        }
                    }

                    final TrendTopic topic = TrendTopic.builder()
                            .topic(node.path("topic").asString(""))
                            .summary(node.path("summary").asString(""))
                            .reasoning(node.path("reasoning").asString(""))
                            .category(node.path("category").asString(""))
                            .mentionCount(node.path("mentionCount").asInt(0))
                            .trendScore(node.path("trendScore").asDouble(0.0))
                            .primaryPlatform(platform)
                            .samplePostIds(String.join(",", relatedIds))
                            .analysis(analysis)
                            .build();

                    topics.add(topic);
                } catch (final Exception e) {
                    log.error("Failed to parse trend topic: {}", node, e);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to parse Anthropic response: {}", rawReponse, e);
        }
        return topics;
    }

    private Platform parsePlatform(final String primaryPlatform) {
        try {
            return Platform.valueOf(primaryPlatform.toUpperCase());
        } catch (final Exception e) {
            return null;
        }
    }
    
}