package com.daesabu.meongcoach.auth.adapter.webapi;

import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenCookies {

	static final String NAME = "refresh_token";

	private static final String PATH = "/api/auth";
	private static final String SAME_SITE = "Strict";
	private static final String EXPIRED_VALUE = "";

	private final Duration refreshTokenValidity;

	RefreshTokenCookies(@Value("${meongcoach.jwt.refresh-token-validity}") Duration refreshTokenValidity) {
		this.refreshTokenValidity = refreshTokenValidity;
	}

	void issue(HttpServletResponse response, String refreshToken) {
		write(response, refreshToken, refreshTokenValidity);
	}

	void expire(HttpServletResponse response) {
		write(response, EXPIRED_VALUE, Duration.ZERO);
	}

	String require(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		return refreshToken;
	}

	private void write(HttpServletResponse response, String value, Duration maxAge) {
		ResponseCookie cookie = ResponseCookie.from(NAME, value)
				.httpOnly(true)
				.secure(true)
				.sameSite(SAME_SITE)
				.path(PATH)
				.maxAge(maxAge)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
