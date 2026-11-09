package com.daesabu.meongcoach.support.adapter.websocket;

import com.daesabu.meongcoach.shared.security.CorsProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketConfigurer {
	private final ChatWebSocketHandler handler;
	private final ChatHandshakeInterceptor authentication;
	private final CorsProperties cors;

	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String[] origins = cors.allowedOriginPatterns().toArray(String[]::new);
		registry.addHandler(handler, "/ws/support")
				.addInterceptors(authentication)
				.setAllowedOriginPatterns(origins);
	}
}
