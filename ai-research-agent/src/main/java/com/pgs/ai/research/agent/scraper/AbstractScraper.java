package com.pgs.ai.research.agent.scraper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.pgs.ai.research.agent.config.ProxyConfig;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@Slf4j
public class AbstractScraper {

	private final ProxyConfig proxyConfig;

	public AbstractScraper(final ProxyConfig proxyConfig) {
		this.proxyConfig = proxyConfig;
	}

	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder().proxy(this.proxyConfig.toProxy())
				.proxyAuthenticator((route, response) -> response.request().newBuilder()
						.header("Proxy-Authorization",
								Credentials.basic(this.proxyConfig.getUsername(),
										this.proxyConfig.getPassword()))
						.build())
				.connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
	}

	public String fetch(final String url) throws IOException {
		final Request request = new Request.Builder().url(url).header(
				"User-Agent", this.proxyConfig.getUserAgent()
			)
			.build();

		try (final Response response = okHttpClient().newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("HTTP code " + response.code() + " for " + url);
			}

			assert response.body() != null;
			return response.body().string();
		}
	}

	public String detectProxyIp() {
		try {
			final String body = fetch("https://api.ipify.org/");
			return body.trim();
		} catch (final IOException e) {
			log.debug("Failed to detect proxy IP", e);
			return "unknown";
		}
	}
}
