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
	 * RevenueCat 조회는 이 요청에서 DB를 처음 건드리기 전에 끝낸다. open-in-view가 켜져 있어 한번 빌린 커넥션은 트랜잭션이 끝나도
	 * 요청이 끝날 때까지 반납되지 않으므로, 회원 확인을 먼저 하면 외부 호출 동안 커넥션을 쥐게 된다 (learning/OsivConnectionLearningTest).
	 * 이 앞에 DB 접근을 넣지 않는다. 대신 탈퇴 회원도 RevenueCat을 한 번 조회한다.
	 * 반영은 EntitlementModifyService의 트랜잭션에 맡긴다. 결제 직후 앱 호출과 웹훅이 동시에 같은 이용권을 새로 만들면
	 * (user_id, type) 유니크 제약으로 한쪽이 실패하는데, 그때는 먼저 커밋된 행을 읽어 한 번 더 맞춘다. 같은 활성 종류로 맞추므로 두 번째는 새로 만들 행이 없다.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void synchronize(Long userId) {
		Set<EntitlementType> activeTypes = activeEntitlementReader.readActiveTypes(userId);
		if (!userFinder.isActiveUser(userId)) {
			log.warn("탈퇴한 회원이라 이용권을 동기화하지 않음: userId={}", userId);
			return;
		}

		Instant now = Instant.now();
		try {
			entitlementModifyService.synchronize(userId, activeTypes, now);
		} catch (DataIntegrityViolationException e) {
			log.warn("동시에 부여된 이용권이 있어 다시 동기화함: userId={}", userId);
			entitlementModifyService.synchronize(userId, activeTypes, now);
		}
	}
}
