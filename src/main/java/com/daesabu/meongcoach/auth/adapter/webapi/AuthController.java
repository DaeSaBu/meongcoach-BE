package com.daesabu.meongcoach.auth.adapter.webapi;

import com.daesabu.meongcoach.auth.adapter.webapi.dto.AccessTokenResponse;
import com.daesabu.meongcoach.auth.adapter.webapi.dto.TokenResponse;
import com.daesabu.meongcoach.auth.application.provided.Authenticator;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.LogoutRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.shared.security.AppVersionFilter;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String WEB_PLATFORM = AppVersionFilter.APP_PLATFORM_HEADER + "=web";

	private final Authenticator authenticator;
	private final RefreshTokenCookies refreshTokenCookies;

	@PostMapping("/login/social")
	public TokenResponse loginSocialAccount(@Valid @RequestBody SocialLoginRequest request) {
		AuthToken authToken = authenticator.socialLogin(request);
		return TokenResponse.from(authToken);
	}

	@PostMapping(value = "/login/social", headers = WEB_PLATFORM)
	public AccessTokenResponse loginSocialAccountOnWeb(@Valid @RequestBody SocialLoginRequest request,
	                                                   HttpServletResponse response) {
		AuthToken authToken = authenticator.socialLogin(request);
		refreshTokenCookies.issue(response, authToken.refreshToken());
		return AccessTokenResponse.from(authToken);
	}

	@PostMapping("/login/email")
	public TokenResponse loginEmailAccount(@Valid @RequestBody EmailLoginRequest request) {
		AuthToken authToken = authenticator.emailLogin(request);
		return TokenResponse.from(authToken);
	}

	@PostMapping(value = "/login/email", headers = WEB_PLATFORM)
	public AccessTokenResponse loginEmailAccountOnWeb(@Valid @RequestBody EmailLoginRequest request,
	                                                  HttpServletResponse response) {
		AuthToken authToken = authenticator.emailLogin(request);
		refreshTokenCookies.issue(response, authToken.refreshToken());
		return AccessTokenResponse.from(authToken);
	}

	@PostMapping("/token/refresh")
	public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		AuthToken authToken = authenticator.refresh(request);
		return TokenResponse.from(authToken);
	}

	@PostMapping(value = "/token/refresh", headers = WEB_PLATFORM)
	public AccessTokenResponse refreshOnWeb(
			@CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
			HttpServletResponse response) {
		TokenRefreshRequest request = new TokenRefreshRequest(refreshTokenCookies.require(refreshToken));
		AuthToken authToken = authenticator.refresh(request);
		refreshTokenCookies.issue(response, authToken.refreshToken());
		return AccessTokenResponse.from(authToken);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request) {
		authenticator.logout(request);
	}

	@PostMapping(value = "/logout", headers = WEB_PLATFORM)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutOnWeb(@CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
	                        HttpServletResponse response) {
		LogoutRequest request = new LogoutRequest(refreshTokenCookies.require(refreshToken));
		authenticator.logout(request);
		refreshTokenCookies.expire(response);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@CurrentUserId Long userId, @RequestBody(required = false) WithdrawRequest request) {
		authenticator.withdraw(userId, request);
	}
}
