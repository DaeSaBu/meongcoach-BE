package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.user.adapter.webapi.dto.GrantType;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenIssueRequest;
import com.daesabu.meongcoach.user.adapter.webapi.dto.TokenResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final Map<GrantType, TokenIssueHandler> handlers;

	public AuthController(List<TokenIssueHandler> handlers) {
		this.handlers = handlers.stream()
				.collect(Collectors.toUnmodifiableMap(TokenIssueHandler::grantType, Function.identity()));
		// switch가 주던 완전성 검사를 부팅 시점 검사로 대체한다. grant를 추가하고 핸들러를 빠뜨리면 기동에 실패한다
		if (this.handlers.size() != GrantType.values().length) {
			throw new IllegalStateException("모든 GrantType에 대한 TokenIssueHandler가 등록되어야 합니다");
		}
	}

	// OAuth2 token endpoint처럼 토큰 컬렉션 하나에 POST하고 발급 방식은 grantType이 구분한다.
	// 소셜 로그인의 회원 조회·생성은 클라이언트가 관찰할 수 없는 부수 효과이므로 계약은 토큰 발급으로 유지한다
	@PostMapping("/tokens")
	public TokenResponse issueToken(@Valid @RequestBody TokenIssueRequest request) {
		return handlers.get(request.grantType()).handle(request);
	}
}
