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
 * 이용권 동기화의 쓰기 단계. 외부 호출을 트랜잭션 밖에서 끝낸 EntitlementSyncService가 호출하도록 별도 빈으로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementModifyService {

	private final EntitlementRepository entitlementRepository;

	// 기존 행의 회수·복구는 변경 감지로 반영되고, 새로 부여한 이용권만 저장한다
	@Transactional
	public void synchronize(Long userId, Set<EntitlementType> activeTypes, Instant now) {
		Entitlements entitlements = new Entitlements(entitlementRepository.findAllByUserId(userId));
		List<Entitlement> granted = entitlements.synchronize(userId, activeTypes, now);
		entitlementRepository.saveAll(granted);
	}
}
