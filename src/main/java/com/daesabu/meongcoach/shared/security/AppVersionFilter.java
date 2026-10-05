package com.daesabu.meongcoach.shared.security;

import com.daesabu.meongcoach.shared.exception.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class AppVersionFilter extends OncePerRequestFilter {

	public static final String APP_VERSION_HEADER = "X-App-Version";
	public static final String APP_PLATFORM_HEADER = "X-App-Platform";

	private static final String API_PATH_PREFIX = "/api/";
	private static final String HEALTH_PATH = "/api/health";

	private final AppVersionProperties properties;
	private final HandlerExceptionResolver resolver;

	public AppVersionFilter(AppVersionProperties properties, HandlerExceptionResolver resolver) {
		this.properties = properties;
		this.resolver = resolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !path.startsWith(API_PATH_PREFIX) || path.equals(HEALTH_PATH);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		try {
			properties.verify(request.getHeader(APP_PLATFORM_HEADER), request.getHeader(APP_VERSION_HEADER));
		} catch (DomainException e) {
			resolver.resolveException(request, response, null, e);
			return;
		}
		filterChain.doFilter(request, response);
	}
}
