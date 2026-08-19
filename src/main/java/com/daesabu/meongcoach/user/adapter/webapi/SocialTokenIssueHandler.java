package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.GrantType;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenIssueRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenResponse;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialTokenIssueHandler implements TokenIssueHandler {

	private final SocialLogin socialLogin;

	@Override
	public GrantType grantType() {
		return GrantType.SOCIAL;
	}

	@Override
	public TokenResponse handle(TokenIssueRequest request) {
		SocialProvider provider = SocialProvider.from(request.provider());
		SocialLoginResult result = socialLogin.login(provider, request.token());
		return TokenResponse.from(result);
	}
}
