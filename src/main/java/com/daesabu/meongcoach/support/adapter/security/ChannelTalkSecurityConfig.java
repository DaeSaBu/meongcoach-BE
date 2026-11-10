package com.daesabu.meongcoach.support.adapter.security;

import com.daesabu.meongcoach.support.application.ChannelTalkProperties;
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
class ChannelTalkSecurityConfig {

	private static final String WEBHOOK_PATH = "/webhooks/channel-talk/support-replies";

	@Bean
	@Order(1)
	SecurityFilterChain channelTalkSecurityFilterChain(HttpSecurity http, ChannelTalkProperties properties,
	                                                AuthenticationEntryPoint authenticationEntryPoint) {
		return http
				.securityMatcher(WEBHOOK_PATH)
				.addFilterBefore(new ChannelTalkAuthenticationFilter(properties), AuthorizationFilter.class)
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
