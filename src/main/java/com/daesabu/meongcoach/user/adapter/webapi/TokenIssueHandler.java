package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.GrantType;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenIssueRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenResponse;

/**
 * grantType별 토큰 발급 처리. 새 grant를 추가할 때는 구현체만 추가하면 되고 컨트롤러는 바뀌지 않는다.
 */
public interface TokenIssueHandler {

	GrantType grantType();

	TokenResponse handle(TokenIssueRequest request);
}
