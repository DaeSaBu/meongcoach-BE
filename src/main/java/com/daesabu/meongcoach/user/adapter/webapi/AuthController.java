package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.LoginRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.LoginResponse;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshResponse;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final SocialLogin socialLogin;
	private final TokenRefresher tokenRefresher;

	// 소셜 로그인의 회원 조회·생성은 클라이언트가 관찰할 수 없는 부수 효과이므로 계약은 로그인(토큰 발급)으로 유지한다
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		SocialProvider provider = SocialProvider.from(request.provider());
		SocialLoginResult result = socialLogin.login(provider, request.token());
		return LoginResponse.from(result);
	}

	@PostMapping("/token/refresh")
	public TokenRefreshResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		AuthToken token = tokenRefresher.refresh(request.refreshToken());
		return TokenRefreshResponse.from(token);
	}
}
