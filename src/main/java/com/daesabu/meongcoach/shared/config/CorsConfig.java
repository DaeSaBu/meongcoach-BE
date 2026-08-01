package com.daesabu.meongcoach.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 프로파일별 웹 프론트엔드 origin만 허용하는 CORS 구성.
 */
@Configuration
@EnableConfigurationProperties(CorsConfig.CorsProperties.class)
public class CorsConfig {

	@Bean
	FilterRegistrationBean<CorsFilter> corsFilter(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(properties.allowedOriginPatterns());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	@ConfigurationProperties("meongcoach.cors")
	public record CorsProperties(List<String> allowedOriginPatterns) {

		public CorsProperties {
			allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
		}
	}
}
