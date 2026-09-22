package com.daesabu.meongcoach.auth.adapter.webapi.legacy;

import com.daesabu.meongcoach.auth.domain.AuthToken;

/**
 * 구 클라이언트의 로그인 응답. 신 계약은 토큰만 내리고 온보딩 여부를 GET /api/users/me로 분리했지만, 구 앱은 로그인 응답에서 바로 읽는다.
 */
public record LegacyLoginResponse(String accessToken, String refreshToken, boolean needsOnboarding) {
	public static LegacyLoginResponse of(AuthToken token, boolean needsOnboarding) {
		return new LegacyLoginResponse(token.accessToken(), token.refreshToken(), needsOnboarding);
	}
}
