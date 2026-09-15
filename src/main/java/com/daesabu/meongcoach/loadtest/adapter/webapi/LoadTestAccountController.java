package com.daesabu.meongcoach.loadtest.adapter.webapi;

import com.daesabu.meongcoach.loadtest.adapter.webapi.dto.LoadTestAccountCreateRequest;
import com.daesabu.meongcoach.loadtest.adapter.webapi.dto.LoadTestAccountResponse;
import com.daesabu.meongcoach.loadtest.domain.exception.InvalidLoadTestKeyException;
import com.daesabu.meongcoach.user.application.provided.LocalAccountRegister;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트용 로컬 계정 생성 API. 토큰이 없는 상태에서 계정을 만드는 것이 목적이라 인증 대신 공유 키 헤더로 보호한다.
 * 빈 등록은 이 클래스의 조건이, 경로 개방은 SecurityConfig가 같은 프로퍼티(meongcoach.loadtest.enabled)로 결정한다.
 */
@RestController
@RequestMapping("/api/loadtest/accounts")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meongcoach.loadtest", name = "enabled", havingValue = "true")
public class LoadTestAccountController {

	static final String API_KEY_HEADER = "X-Loadtest-Key";

	private final LocalAccountRegister localAccountRegister;
	private final LoadTestProperties properties;

	// 본문 검증(@Valid)이 키 검사보다 먼저 실행되므로 키가 틀려도 형식 오류는 400으로 먼저 응답된다. 공개 정보라 감수한다
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoadTestAccountResponse createAccount(@RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
	                                             @Valid @RequestBody LoadTestAccountCreateRequest request) {
		if (!properties.matches(apiKey)) {
			throw new InvalidLoadTestKeyException();
		}
		Long userId = localAccountRegister.register(request.toInfo());
		return new LoadTestAccountResponse(userId);
	}
}
