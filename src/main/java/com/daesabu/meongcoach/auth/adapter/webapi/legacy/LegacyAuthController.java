package com.daesabu.meongcoach.auth.adapter.webapi.legacy;

import com.daesabu.meongcoach.auth.application.provided.Authenticator;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구 클라이언트(신 인증 계약 이전에 배포된 앱)가 호출하는 경로·본문·응답을 신 구현(Authenticator) 위에서 그대로 유지한다.
 * 구 앱 지원이 끝나면 이 패키지 전체를 삭제하고, 다음도 함께 지운다.
 * <ul>
 * <li>{@code SecurityConfig}의 구 로그인 permitAll 경로 2개와 {@code LEGACY_WITHDRAW_PATH}</li>
 * <li>{@code SecurityFilterChainTest}의 구 경로 케이스</li>
 * <li>{@code build.gradle.kts} {@code publicPaths}의 구 경로 2개</li>
 * <li>{@code index.adoc}의 구 클라이언트 호환 절, {@code docs/security.md}·{@code docs/error-handling.md}의 레거시 표기</li>
 * <li>{@code UserFinder.isOnboardingUser} (다른 사용처가 없으면)</li>
 * </ul>
 * 토큰 재발급·로그아웃은 계약이 바뀌지 않아 신 컨트롤러가 그대로 받는다.
 */
@RestController
@RequiredArgsConstructor
public class LegacyAuthController {

	private final Authenticator authenticator;
	private final UserFinder userFinder;

	// 구 계약은 제공자를 경로로, 토큰을 `token` 필드로 받는다. 신 요청 DTO로 옮겨 담는 일은 이 호환 계층만 한다
	@PostMapping("/api/auth/login/social/{provider}")
	public LegacyLoginResponse loginSocialAccount(@PathVariable String provider,
	                                              @Valid @RequestBody LegacySocialLoginRequest request) {
		SocialLoginRequest socialLoginRequest = new SocialLoginRequest(provider, request.token());
		AuthToken authToken = authenticator.socialLogin(socialLoginRequest);
		return loginResponse(authToken);
	}

	// 본문 구조가 신 계약과 같아 요청 DTO를 그대로 쓴다
	@PostMapping("/api/auth/login/local")
	public LegacyLoginResponse loginEmailAccount(@Valid @RequestBody EmailLoginRequest request) {
		AuthToken authToken = authenticator.emailLogin(request);
		return loginResponse(authToken);
	}

	@DeleteMapping("/api/users/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@CurrentUserId Long userId, @RequestBody(required = false) WithdrawRequest request) {
		authenticator.withdraw(userId, request);
	}

	// 구 앱은 로그인 응답의 needsOnboarding으로 화면을 분기하므로 신 계약의 GET /api/users/me 결과를 응답에 합친다
	private LegacyLoginResponse loginResponse(AuthToken authToken) {
		boolean needsOnboarding = userFinder.isOnboardingUser(authToken.userId());
		return LegacyLoginResponse.of(authToken, needsOnboarding);
	}
}
