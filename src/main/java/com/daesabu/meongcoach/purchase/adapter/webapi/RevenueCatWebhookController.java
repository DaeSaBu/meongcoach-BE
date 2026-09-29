package com.daesabu.meongcoach.purchase.adapter.webapi;

import com.daesabu.meongcoach.purchase.adapter.webapi.dto.RevenueCatWebhookRequest;
import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RevenueCat 웹훅 수신. 발신자 확인은 RevenueCatWebhookAuthInterceptor가 먼저 끝낸다.
 * 처리하지 않는 이벤트와 저장할 수 없는 구매도 200으로 끝낸다.
 * 200이 아니면 RevenueCat이 같은 이벤트를 최대 5회 재전송하는데, 이런 이벤트는 다시 받아도 결과가 같기 때문이다.
 * 없는 회원의 구매만 예외로 실패시킨다. 실패한 이벤트는 대시보드에 남아, 회원 매핑을 고친 뒤 Retry로 다시 받아 복구한다.
 */
@RestController
@RequestMapping(RevenueCatWebhookController.PATH)
@RequiredArgsConstructor
public class RevenueCatWebhookController {

	static final String PATH = "/api/webhooks/revenuecat";

	private final RevenueCatPurchaseTranslator translator;
	private final PurchaseRegister purchaseRegister;

	@PostMapping
	public void receive(@Valid @RequestBody RevenueCatWebhookRequest request) {
		Optional<PurchaseRegisterRequest> purchaseRegisterRequest = translator.translate(request.event());
		purchaseRegisterRequest.ifPresent(purchaseRegister::register);
	}
}
