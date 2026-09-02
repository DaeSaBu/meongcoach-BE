package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.LocalLoginRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.LoginResponse;
import com.daesabu.meongcoach.user.adapter.webapi.dto.LogoutRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.SocialLoginRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshResponse;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.LocalLogin;
import com.daesabu.meongcoach.user.application.provided.LoginResult;
import com.daesabu.meongcoach.user.application.provided.Logout;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final SocialLogin socialLogin;
	private final LocalLogin localLogin;
	private final TokenRefresher tokenRefresher;
	private final Logout logout;

	// 소셜 로그인의 회원 조회·생성은 클라이언트가 관찰할 수 없는 부수 효과이므로 계약은 로그인(토큰 발급)으로 유지한다
	// 제공자는 경로 변수로 받아 구글·애플이 추가돼도 요청 본문 계약이 바뀌지 않게 한다
	@PostMapping("/login/social/{provider}")
	public LoginResponse login(@PathVariable String provider, @Valid @RequestBody SocialLoginRequest request) {
		LoginResult result = socialLogin.login(SocialProvider.from(provider), request.token());
		return LoginResponse.from(result);
	}

	// 스토어 심사용 테스트 계정 전용. 가입 API가 없으므로 시드된 계정만 로그인할 수 있다
	@PostMapping("/login/local")
	public LoginResponse loginLocal(@Valid @RequestBody LocalLoginRequest request) {
		LoginResult result = localLogin.login(request.email(), request.password());
		return LoginResponse.from(result);
	}

	@PostMapping("/token/refresh")
	public TokenRefreshResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		AuthToken token = tokenRefresher.refresh(request.refreshToken());
		return TokenRefreshResponse.from(token);
	}

	// 액세스 토큰이 만료된 뒤에도 로그아웃할 수 있어야 하므로 재발급과 같이 인증 없이 리프레시 토큰만 받는다.
	// 폐기된 토큰을 다시 보내도 204라 클라이언트는 응답과 무관하게 보관 중인 토큰을 버리면 된다
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request) {
		logout.logout(request.refreshToken());
	}
}
