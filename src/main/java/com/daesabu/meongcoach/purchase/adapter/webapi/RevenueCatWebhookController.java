package com.daesabu.meongcoach.purchase.adapter.webapi;

import com.daesabu.meongcoach.purchase.adapter.webapi.dto.RevenueCatWebhookRequest;
import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.domain.exception.WebhookUnauthorizedException;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RevenueCat 웹훅 수신. 처리하지 않는 이벤트와 저장할 수 없는 구매도 200으로 끝낸다.
 * 2xx가 아니면 RevenueCat이 같은 이벤트를 재전송하는데, 이런 이벤트는 다시 받아도 결과가 같기 때문이다.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/revenuecat")
@RequiredArgsConstructor
public class RevenueCatWebhookController {

	private final RevenueCatWebhookProperties properties;
	private final PurchaseRegister purchaseRegister;

	@PostMapping
	public void receive(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody RevenueCatWebhookRequest request
	) {
		if (!properties.authorizes(authorization)) {
			throw new WebhookUnauthorizedException();
		}
		RevenueCatWebhookRequest.Event event = request.event();
		if (!event.isNonRenewingPurchase()) {
			log.info("처리하지 않는 RevenueCat 이벤트: type={}, eventId={}", event.type(), event.id());
			return;
		}
		Optional<Long> userId = event.findUserId();
		// 앱이 로그인 전 구매를 막는 것이 전제라, 여기로 오면 결제는 됐는데 이용권이 없는 사고다. transactionId로 수동 복구한다
		if (userId.isEmpty()) {
			log.error("회원이 아닌 사용자의 구매라 저장하지 않음: appUserId={}, transactionId={}", event.appUserId(),
					event.transactionId());
			return;
		}

		purchaseRegister.register(event.toPurchaseRegisterRequest(userId.get()));
	}
}
