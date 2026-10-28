package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.SlotPreference;
import java.util.List;

/**
 * 회원의 알림 설정 조회 공개 API.
 */
public interface NotificationPreferenceFinder {

	/**
	 * 회원의 시간대별 알림 설정을 시간대 순서로 반환한다. 행이 없는 시간대는 기본값으로 채우고 행을 만들지 않는다.
	 */
	List<SlotPreference> findSlotPreferences(Long userId);
}
