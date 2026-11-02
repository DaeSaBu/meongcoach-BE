package com.daesabu.meongcoach.ai.adapter.integration;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GeminiClientConfiguration {

	@Bean
	Client geminiClient(GeminiProperties properties) {
		HttpOptions httpOptions = HttpOptions.builder()
				.timeout((int) properties.responseTimeout().toMillis())
				.build();
		return Client.builder()
				.apiKey(properties.apiKey())
				.httpOptions(httpOptions)
				.build();
	}
}
