package com.daesabu.meongcoach.notification.adapter.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
class InternalSecurityConfig {

	private static final String INTERNAL_PATH_PATTERN = "/internal/**";

	@Bean
	@Order(0)
	SecurityFilterChain internalSecurityFilterChain(HttpSecurity http, InternalNotificationProperties properties,
	                                                AuthenticationEntryPoint authenticationEntryPoint) {
		return http
				.securityMatcher(INTERNAL_PATH_PATTERN)
				.addFilterBefore(new InternalSecretAuthenticationFilter(properties), AuthorizationFilter.class)
				.cors(AbstractHttpConfigurer::disable)
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
				.build();
	}
}
