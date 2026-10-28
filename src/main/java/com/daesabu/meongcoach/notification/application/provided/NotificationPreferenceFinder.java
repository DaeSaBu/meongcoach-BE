package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.HourPreference;
import java.util.List;

/**
 * 회원의 알림 설정 조회 공개 API.
 */
public interface NotificationPreferenceFinder {

	/**
	 * 회원의 시각별 알림 설정을 허용 시각(8·11·17시) 순서로 반환한다. 행이 없는 시각은 기본값으로 채우고 행을 만들지 않는다.
	 */
	List<HourPreference> findHourPreferences(Long userId);
}
