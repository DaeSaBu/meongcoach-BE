package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import jakarta.validation.Valid;

/**
 * 회원의 기기 토큰과 알림 설정을 바꾸는 공개 API.
 */
public interface NotificationRegister {

	/**
	 * 회원의 기기 토큰을 저장한다. 같은 토큰이 이미 있으면 그 행의 회원과 마지막 확인 시각을 요청 회원과 현재 시각으로 바꾼다.
	 * 회원에게 알림 설정 행이 없으면 8·11·17시의 기본값으로 세 행을 만든다.
	 * 플랫폼이 IOS·ANDROID가 아니면 {@code InvalidDevicePlatformException}을 던진다.
	 */
	void registerDeviceToken(Long userId, @Valid DeviceTokenRegisterRequest request);

	/**
	 * 회원 본인의 기기 토큰을 지운다. 토큰이 없거나 다른 회원의 토큰이면 아무것도 하지 않는다.
	 */
	void deleteDeviceToken(Long userId, @Valid DeviceTokenUnregisterRequest request);

	/**
	 * 목록의 기기 토큰을 회원과 무관하게 모두 지운다. 없는 토큰은 무시한다.
	 */
	void deleteDeviceTokens(@Valid DeviceTokenDeleteRequest request);

	/**
	 * 회원의 한 시각 알림 수신 여부를 요청 값으로 바꾼다. 회원·시각 행이 있으면 값만 바꾸고, 없으면 새로 만든다.
	 * 시각이 허용 시각(8·11·17시)이 아니면 {@code NotAllowedNotificationHourException}을 던진다.
	 */
	void updatePreference(Long userId, int hour, @Valid NotificationPreferenceUpdateRequest request);

	/**
	 * 회원의 기기 토큰과 알림 설정을 모두 지운다.
	 */
	void deleteAllByUserId(Long userId);
}
