package com.daesabu.meongcoach.purchase.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementGranter;
import com.daesabu.meongcoach.entitlement.application.provided.dto.EntitlementGrantRequest;
import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.PurchaseRepository;
import com.daesabu.meongcoach.purchase.domain.Purchase;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
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

	private final UserFinder userFinder;
	private final PurchaseRepository purchaseRepository;
	private final EntitlementGranter entitlementGranter;

	/**
	 * 구매를 기록하고 스토어가 알려 준 권한을 entitlement 모듈로 부여한다. 모놀리스에서는 같은 트랜잭션으로 묶여 구매만 남지 않는다.
	 * 같은 구매가 여러 번 전달될 수 있어, 이미 처리한 거래와 활성 회원이 아닌 사용자의 구매는 저장하지 않고 정상 종료한다.
	 * 같은 거래가 동시에 두 번 오면 transaction_id 유니크 제약으로 한쪽이 실패하고, 다시 전달될 때 이미 처리한 거래로 걸러진다.
	 */
	@Override
	@Transactional
	public void register(PurchaseRegisterRequest purchaseRegisterRequest) {
		if (isDuplicateTransactionId(purchaseRegisterRequest)) {
			log.warn("이미 처리된 결제건 입니다 transactionId={}", purchaseRegisterRequest.transactionId());
			return;
		}
		Long userId = purchaseRegisterRequest.userId();
		if (!userFinder.isActiveUser(userId)) {
			log.warn("활성 회원이 아닌 사용자의 구매라 저장하지 않음: userId={}, transactionId={}", userId,
					purchaseRegisterRequest.transactionId());
			return;
		}

		Purchase purchase = purchaseRepository.save(Purchase.register(userId, purchaseRegisterRequest.toCommand()));
		entitlementGranter.grant(
				new EntitlementGrantRequest(userId, purchase.getId(), purchaseRegisterRequest.entitlementIds())
		);
	}

	private boolean isDuplicateTransactionId(PurchaseRegisterRequest purchaseRegisterRequest) {
		return purchaseRepository.existsByTransactionId(purchaseRegisterRequest.transactionId());
	}
}
