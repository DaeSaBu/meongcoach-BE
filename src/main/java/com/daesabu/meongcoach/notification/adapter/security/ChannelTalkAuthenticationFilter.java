package com.daesabu.meongcoach.notification.adapter.security;

import com.daesabu.meongcoach.notification.application.ChannelTalkProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class ChannelTalkAuthenticationFilter extends OncePerRequestFilter {

	private static final String TOKEN_PARAMETER = "token";

	private static final String PRINCIPAL = "channel-talk";
	private static final String AUTHORITY = "ROLE_CHANNEL_TALK";

	private final ChannelTalkProperties properties;

	public ChannelTalkAuthenticationFilter(ChannelTalkProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		if (properties.matches(request.getParameter(TOKEN_PARAMETER))) {
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new PreAuthenticatedAuthenticationToken(PRINCIPAL, null,
					List.of(new SimpleGrantedAuthority(AUTHORITY))));
			SecurityContextHolder.setContext(context);
		}
		filterChain.doFilter(request, response);
	}
}
