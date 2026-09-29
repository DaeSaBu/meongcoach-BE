package com.daesabu.meongcoach.entitlement.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementSyncService implements EntitlementSynchronizer {

	private final UserFinder userFinder;
	private final ActiveEntitlementReader activeEntitlementReader;
	private final EntitlementModifyService entitlementModifyService;

	/**
	 * 외부 호출을 기다리는 동안 DB 커넥션을 잡지 않도록 트랜잭션 없이 활성 이용권을 읽고, 반영은 EntitlementModifyService의 트랜잭션에 맡긴다.
	 * 결제 직후 앱 호출과 웹훅이 동시에 같은 이용권을 새로 만들면 (user_id, type) 유니크 제약으로 한쪽이 실패한다.
	 * 그때는 먼저 커밋된 행을 읽어 한 번 더 맞춘다. 같은 활성 종류로 맞추므로 두 번째는 새로 만들 행이 없다.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void synchronize(Long userId) {
		if (!userFinder.isActiveUser(userId)) {
			log.warn("탈퇴한 회원이라 이용권을 동기화하지 않음: userId={}", userId);
			return;
		}

		Set<EntitlementType> activeTypes = activeEntitlementReader.readActiveTypes(userId);
		Instant now = Instant.now();
		try {
			entitlementModifyService.synchronize(userId, activeTypes, now);
		} catch (DataIntegrityViolationException e) {
			log.warn("동시에 부여된 이용권이 있어 다시 동기화함: userId={}", userId);
			entitlementModifyService.synchronize(userId, activeTypes, now);
		}
	}
}
