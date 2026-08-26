package com.pgs.ai.research.agent.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
	name = "scraped_posts",
	uniqueConstraints = {
	        @UniqueConstraint(columnNames = {"platform", "externalId"})
	}
)
public class ScrapedPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Platform platform;

	@Column(nullable = false)
	private String externalId;

	@Column(nullable = false, length = 1024)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String content;

	@Column(length = 2048)
	private String url;

	private String author;

	private int score;

	private int commentCount;

	@Column(length = 512)
	private String subReddit;

	private String proxyIpUsed;

	private LocalDateTime postedAt;

	private LocalDateTime scrapedAt;

	@PrePersist
	public void prePersist() {
		this.scrapedAt = LocalDateTime.now();
	}
}