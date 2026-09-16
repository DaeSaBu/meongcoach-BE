package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshResponse;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JMeter 부하 측정용 임시 API. AuthController의 재발급과 같은 로직을 /api/test 경로로 노출해 다른 측정 대상과 경로 체계를 맞춘다.
 * 재발급은 rotation이라 리프레시 토큰 하나당 한 번만 성공한다. 측정이 끝나면 SecurityConfig의 /api/test/** 허용과 함께 제거한다.
 */
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
public class AuthLoadTestController {

	private final TokenRefresher tokenRefresher;

	@PostMapping("/token/refresh")
	public TokenRefreshResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		AuthToken token = tokenRefresher.refresh(request.refreshToken());
		return TokenRefreshResponse.from(token);
	}
}
