package com.daesabu.meongcoach.notification.application.provided;

import java.util.List;

/**
 * 시각별 푸시 발송 대상 조회 공개 API.
 */
public interface NotificationTargetFinder {

	/**
	 * 해당 시각 알림을 켠 활성 회원의 기기 FID을 한 행씩 반환한다. 회원 ID 오름차순, 같은 회원 안에서는 FID 등록 순이다.
	 * FID이 없는 회원은 결과에서 빠진다.
	 * 시각이 0~23 밖이면 {@code InvalidNotificationHourException}을 던진다.
	 */
	List<NotificationTargetResult> findTargets(int hour);
}
