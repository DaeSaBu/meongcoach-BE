package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.GrantType;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenIssueRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenResponse;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenIssueHandler implements TokenIssueHandler {

	private final TokenRefresher tokenRefresher;

	@Override
	public GrantType grantType() {
		return GrantType.REFRESH;
	}

	@Override
	public TokenResponse handle(TokenIssueRequest request) {
		AuthToken token = tokenRefresher.refresh(request.refreshToken());
		return TokenResponse.from(token);
	}
}
