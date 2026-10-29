package com.daesabu.meongcoach.notification.adapter.security;

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

public class InternalSecretAuthenticationFilter extends OncePerRequestFilter {

	public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

	private static final String PRINCIPAL = "notification-sender";
	private static final String AUTHORITY = "ROLE_INTERNAL";

	private final InternalNotificationProperties properties;

	public InternalSecretAuthenticationFilter(InternalNotificationProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		if (properties.matches(request.getHeader(INTERNAL_SECRET_HEADER))) {
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new PreAuthenticatedAuthenticationToken(PRINCIPAL, null,
					List.of(new SimpleGrantedAuthority(AUTHORITY))));
			SecurityContextHolder.setContext(context);
		}
		filterChain.doFilter(request, response);
	}
}
