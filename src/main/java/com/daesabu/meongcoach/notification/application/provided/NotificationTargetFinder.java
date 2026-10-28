package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.List;

/**
 * 시간대별 푸시 발송 대상 조회 공개 API.
 */
public interface NotificationTargetFinder {

	/**
	 * 해당 시간대 알림을 켠 활성 회원의 기기 토큰을 회원별로 묶어 반환한다. 토큰이 없는 회원은 결과에서 빠진다.
	 */
	List<NotificationTargetResult> findTargets(NotificationSlot slot);
}
