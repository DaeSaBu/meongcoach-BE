package com.daesabu.meongcoach.support.adapter.websocket;

import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
	private final ChatRegister register;

	@Override
	public boolean beforeHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler handler,
			Map<String, Object> attributes) {
		String protocols = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
		if (protocols != null) {
			for (String protocol : protocols.split(",")) {
				if (!protocol.strip().startsWith("ticket.")) {
					continue;
				}
				try {
					UUID ticket = UUID.fromString(protocol.strip().substring(7));
					attributes.put("access", register.consumeTicket(ticket));
					return true;
				} catch (IllegalArgumentException
						| com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException
								exception) {
					response.setStatusCode(HttpStatus.UNAUTHORIZED);
					return false;
				}
			}
		}
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		return false;
	}

	@Override
	public void afterHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler handler,
			Exception exception) {}
}
