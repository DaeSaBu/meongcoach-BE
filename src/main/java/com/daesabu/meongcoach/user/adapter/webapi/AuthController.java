package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.SocialLoginRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.SocialLoginResponse;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenRefreshResponse;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

	private final SocialLogin socialLogin;
	private final TokenRefresher tokenRefresher;

	// 제공자를 경로 변수로 받아 구글·애플을 추가해도 컨트롤러가 바뀌지 않게 한다
	@PostMapping("/social/{provider}")
	public SocialLoginResponse socialLogin(@PathVariable String provider,
	                                       @Valid @RequestBody SocialLoginRequest request) {
		SocialLoginResult result = socialLogin.login(SocialProvider.from(provider), request.token());
		return SocialLoginResponse.from(result);
	}

	@PostMapping("/token/refresh")
	public TokenRefreshResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
		AuthToken token = tokenRefresher.refresh(request.refreshToken());
		return TokenRefreshResponse.from(token);
	}
}
