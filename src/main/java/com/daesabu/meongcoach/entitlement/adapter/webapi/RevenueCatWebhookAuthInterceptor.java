package com.daesabu.meongcoach.entitlement.adapter.webapi;

import com.daesabu.meongcoach.entitlement.domain.exception.WebhookUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RevenueCat 웹훅 발신자 확인. 웹훅 경로는 JWT 없이 열려 있어 이 인증값이 유일한 발신자 확인이다.
 * 본문 역직렬화·검증보다 먼저 실행되도록 컨트롤러가 아니라 여기서 확인한다.
 * 던진 예외는 GlobalExceptionHandler가 처리해 다른 API와 같은 에러 형식으로 응답된다.
 */
@RequiredArgsConstructor
public class RevenueCatWebhookAuthInterceptor implements HandlerInterceptor {

	private final RevenueCatWebhookProperties properties;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!properties.authorizes(authorization)) {
			throw new WebhookUnauthorizedException();
		}
		return true;
	}
}
