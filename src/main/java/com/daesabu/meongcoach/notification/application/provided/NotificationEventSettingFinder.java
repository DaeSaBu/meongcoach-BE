package com.daesabu.meongcoach.notification.application.provided;

import java.util.List;

/**
 * 회원의 이벤트 알림 설정 조회 공개 API.
 */
public interface NotificationEventSettingFinder {

	/**
	 * 회원의 종류별 이벤트 알림 설정을 종류 선언 순서로 반환한다. 행이 없으면 빈 목록을 반환하고 행을 만들지 않는다.
	 */
	List<NotificationEventSettingResult> findEventSettings(Long userId);
}
