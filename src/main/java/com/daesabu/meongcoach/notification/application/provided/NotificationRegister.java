package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import jakarta.validation.Valid;

/**
 * 회원의 기기 토큰과 알림 설정을 바꾸는 공개 API.
 */
public interface NotificationRegister {

	/**
	 * 회원의 기기 토큰을 저장한다. 같은 토큰이 이미 있으면 그 행의 회원과 마지막 확인 시각을 요청 회원과 현재 시각으로 바꾼다.
	 * 회원에게 알림 설정 행이 없으면 시간대별 기본값으로 세 행을 만든다.
	 * 플랫폼이 IOS·ANDROID가 아니면 {@code InvalidDevicePlatformException}을 던진다.
	 */
	void registerDeviceToken(Long userId, @Valid DeviceTokenRegisterRequest request);

	/**
	 * 회원 본인의 기기 토큰을 지운다. 토큰이 없거나 다른 회원의 토큰이면 아무것도 하지 않는다.
	 */
	void deleteDeviceToken(Long userId, String token);

	/**
	 * 목록의 기기 토큰을 회원과 무관하게 모두 지운다. 없는 토큰은 무시한다.
	 */
	void deleteDeviceTokens(@Valid DeviceTokenDeleteRequest request);

	/**
	 * 회원의 시간대별 알림 설정을 요청 값으로 통째로 바꾼다. 행이 없던 시간대는 새로 만든다.
	 * 시간대 이름이 MORNING·NOON·EVENING이 아니면 {@code InvalidNotificationSlotException}을,
	 * 세 시간대가 각각 한 번씩 있지 않으면 {@code InvalidNotificationSlotsException}을 던진다.
	 */
	void updatePreferences(Long userId, @Valid NotificationPreferenceUpdateRequest request);

	/**
	 * 회원의 기기 토큰과 알림 설정을 모두 지운다.
	 */
	void deleteAllByUserId(Long userId);
}
