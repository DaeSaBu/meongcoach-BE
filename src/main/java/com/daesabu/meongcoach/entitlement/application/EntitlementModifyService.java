package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlements;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용권 동기화의 쓰기 단계. EntitlementSyncService가 트랜잭션 없이 외부 호출을 끝낸 뒤 이 빈의 트랜잭션으로 반영한다.
 * 같은 클래스 안의 호출에는 @Transactional이 걸리지 않으므로 별도 빈으로 둔다.
 * RevenueCat을 거치지 않고 이용권 집합을 덮어쓰는 입구가 되면 안 되므로, provided로 공개하지 않고 같은 패키지에서만 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class EntitlementModifyService {

	private final EntitlementRepository entitlementRepository;

	// 기존 행의 회수·복구는 변경 감지로 반영되고, 새로 부여한 이용권만 저장한다
	@Transactional
	void synchronize(Long userId, Set<EntitlementType> activeTypes, Instant now) {
		Entitlements entitlements = new Entitlements(entitlementRepository.findAllByUserId(userId));
		List<Entitlement> granted = entitlements.synchronize(userId, activeTypes, now);
		entitlementRepository.saveAll(granted);
	}
}
