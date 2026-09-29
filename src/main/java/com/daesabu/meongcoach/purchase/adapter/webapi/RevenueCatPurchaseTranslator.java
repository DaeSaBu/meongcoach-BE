package com.daesabu.meongcoach.purchase.adapter.webapi;

import com.daesabu.meongcoach.purchase.adapter.webapi.dto.RevenueCatWebhookRequest;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RevenueCat 웹훅 이벤트 중 구매로 등록할 것만 골라 provided Request로 바꾼다.
 * 이벤트 타입·app_user_id 해석은 RevenueCat 지식이라 application에 넘기지 않고 여기서 끝낸다.
 */
@Slf4j
@Component
public class RevenueCatPurchaseTranslator {

	private static final String NON_RENEWING_PURCHASE = "NON_RENEWING_PURCHASE";

	// 앱이 RevenueCat에 로그인시킨 회원 ID. 로그인 전 구매는 $RCAnonymousID:로 시작하는 익명 ID로 온다.
	// 숫자가 아닌 ID를 Long으로 바꾸다 예외가 나면 RevenueCat이 재전송을 반복하므로 먼저 걸러 낸다. 18자리까지는 Long 범위를 넘지 않는다
	private static final Pattern MEMBER_APP_USER_ID = Pattern.compile("\\d{1,18}");

	public Optional<PurchaseRegisterRequest> translate(RevenueCatWebhookRequest.Event event) {
		// 현재 상품은 평생권(비갱신)뿐이라 이 타입만 구매로 등록한다
		if (!NON_RENEWING_PURCHASE.equals(event.type())) {
			log.info("처리하지 않는 RevenueCat 이벤트: type={}, eventId={}", event.type(), event.id());
			return Optional.empty();
		}
		// 앱이 로그인 전 구매를 막는 것이 전제라, 여기로 오면 결제는 됐는데 이용권이 없는 사고다. transactionId로 수동 복구한다
		if (!isMemberAppUserId(event.appUserId())) {
			log.error("회원이 아닌 사용자의 구매라 저장하지 않음: appUserId={}, transactionId={}", event.appUserId(),
					event.transactionId());
			return Optional.empty();
		}

		Long userId = Long.valueOf(event.appUserId());
		PurchaseRegisterRequest request = event.toPurchaseRegisterRequest(userId);
		return Optional.of(request);
	}

	private boolean isMemberAppUserId(String appUserId) {
		return appUserId != null && MEMBER_APP_USER_ID.matcher(appUserId).matches();
	}
}
