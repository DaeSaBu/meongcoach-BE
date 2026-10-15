package com.daesabu.meongcoach.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 운영자 전용 경로(/api/admin/**)를 X-Admin-Key 헤더로 보호한다.
 * 키가 설정되지 않은 환경에서는 경로 자체가 없는 것처럼 404를 돌려주고,
 * 키가 없거나 다르면 401을 돌려준다. 비교는 길이에 따른 시간 차이가 없는 상수 시간 비교를 쓴다.
 */
public class AdminKeyInterceptor implements HandlerInterceptor {

	public static final String HEADER = "X-Admin-Key";

	private final byte[] adminKey;

	public AdminKeyInterceptor(String adminKey) {
		this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (adminKey.length == 0) {
			throw new NoResourceFoundException(HttpMethod.valueOf(request.getMethod()), request.getRequestURI(), null);
		}
		String provided = request.getHeader(HEADER);
		if (provided == null || !MessageDigest.isEqual(adminKey, provided.getBytes(StandardCharsets.UTF_8))) {
			throw new BadCredentialsException("관리자 키가 올바르지 않습니다.");
		}
		return true;
	}
}
