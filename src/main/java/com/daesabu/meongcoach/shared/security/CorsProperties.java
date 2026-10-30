package com.daesabu.meongcoach.shared.security;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("meongcoach.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

	public CorsProperties {
		allowedOriginPatterns = List.copyOf(Objects.requireNonNullElse(allowedOriginPatterns, List.of()));
	}
}
