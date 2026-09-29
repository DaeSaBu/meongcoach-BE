package com.daesabu.meongcoach.entitlement.adapter.webapi;

import com.daesabu.meongcoach.entitlement.adapter.webapi.dto.RevenueCatWebhookRequest;
import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RevenueCat 웹훅 수신. 발신자 확인은 RevenueCatWebhookAuthInterceptor가 먼저 끝낸다.
 * RevenueCat 권장대로 웹훅은 신호로만 쓰고, 이벤트 종류와 상관없이 관련 회원을 앱 호출과 같은 동기화로 맞춘다.
 * 결제 직후 부여는 앱 호출이 맡고, 웹훅은 환불·계정 이전처럼 앱이 모르는 변화를 반영한다.
 * 상태를 통째로 맞추므로 같은 이벤트를 다시 받아도 결과가 같다. 실패하면 200이 아니므로 RevenueCat이 최대 5회 재전송한다.
 */
@RestController
@RequestMapping(RevenueCatWebhookController.PATH)
@RequiredArgsConstructor
public class RevenueCatWebhookController {

	static final String PATH = "/api/webhooks/revenuecat";

	private final EntitlementSynchronizer entitlementSynchronizer;

	@PostMapping
	public void receive(@Valid @RequestBody RevenueCatWebhookRequest request) {
		request.event().userIds().forEach(entitlementSynchronizer::synchronize);
	}
}
