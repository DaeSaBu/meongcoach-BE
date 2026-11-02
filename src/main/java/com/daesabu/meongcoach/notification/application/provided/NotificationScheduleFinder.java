package com.daesabu.meongcoach.notification.application.provided;

import java.util.List;

/**
 * 회원의 알림 설정 조회 공개 API.
 */
public interface NotificationScheduleFinder {

	/**
	 * 회원의 시각별 알림 설정을 시·분 오름차순으로 반환한다. 행이 없으면 빈 목록을 반환하고 행을 만들지 않는다.
	 */
	List<NotificationScheduleResult> findSchedules(Long userId);
}
