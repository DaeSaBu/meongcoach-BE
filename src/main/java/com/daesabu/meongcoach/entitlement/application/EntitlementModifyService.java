package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.Entitlements;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원의 이용권을 결제 대행이 알려 준 활성 종류에 맞춘다.
 * 결제 대행을 거치지 않고 이용권 집합을 덮어쓰는 입구가 되면 안 되므로, provided로 공개하지 않고 같은 패키지에서만 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class EntitlementModifyService {

	private final EntitlementRepository entitlementRepository;

	// 기존 행의 회수·복구는 변경 감지로 반영되고, 새로 부여한 이용권만 저장한다
	@Transactional
	void synchronize(Long userId, Set<EntitlementType> activeTypes) {
		Entitlements entitlements = new Entitlements(entitlementRepository.findAllByUserId(userId));
		List<Entitlement> granted = entitlements.synchronize(userId, activeTypes);
		entitlementRepository.saveAll(granted);
	}
}
