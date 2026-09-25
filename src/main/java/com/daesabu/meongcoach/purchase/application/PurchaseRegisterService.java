package com.daesabu.meongcoach.purchase.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.entitlement.application.provided.dto.EntitlementGrantRequest;
import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.PurchaseRepository;
import com.daesabu.meongcoach.purchase.domain.Purchase;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseRegisterService implements PurchaseRegister {

	// 앱이 RevenueCat에 로그인시킨 회원 ID. 로그인 전 구매는 $RCAnonymousID:로 시작하는 익명 ID로 온다
	private static final Pattern MEMBER_APP_USER_ID = Pattern.compile("\\d{1,18}");

	private final UserFinder userFinder;
	private final PurchaseRepository purchaseRepository;
	private final EntitlementGranter entitlementGranter;

	/**
	 * 구매를 기록하고 웹훅이 알려 준 권한을 entitlement 모듈로 부여한다. 모놀리스에서는 같은 트랜잭션으로 묶여 구매만 남지 않는다.
	 * 이미 처리한 거래, 익명 사용자, 활성 회원이 아닌 사용자의 구매는 저장하지 않고 끝낸다.
	 * 실패로 끝내면 RevenueCat이 같은 이벤트를 계속 재전송하기 때문이다.
	 * 같은 거래가 동시에 두 번 오면 transaction_id 유니크 제약으로 한쪽이 실패하고, 재전송 때 이미 처리한 거래로 걸러진다.
	 */
	@Override
	@Transactional
	public void register(PurchaseRegisterRequest purchaseRegisterRequest) {
		String transactionId = purchaseRegisterRequest.transactionId();
		if (purchaseRepository.existsByTransactionId(transactionId)) {
			return;
		}
		String appUserId = purchaseRegisterRequest.appUserId();
		if (!MEMBER_APP_USER_ID.matcher(appUserId).matches()) {
			log.warn("회원이 아닌 사용자의 구매라 저장하지 않음: appUserId={}, transactionId={}", appUserId, transactionId);
			return;
		}
		Long userId = Long.valueOf(appUserId);
		if (!userFinder.isActiveUser(userId)) {
			log.warn("활성 회원이 아닌 사용자의 구매라 저장하지 않음: userId={}, transactionId={}", userId, transactionId);
			return;
		}

		Purchase purchase = purchaseRepository.save(Purchase.register(userId, purchaseRegisterRequest.toCommand()));
		entitlementGranter.grant(
				new EntitlementGrantRequest(userId, purchase.getId(), purchaseRegisterRequest.entitlementIds())
		);
	}
}
