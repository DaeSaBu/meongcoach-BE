package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import jakarta.validation.Valid;

/**
 * 회원의 기기 FID와 알림 설정을 바꾸는 공개 API.
 */
public interface NotificationRegister {

	/**
	 * 회원의 기기 FID를 저장한다. 같은 FID가 이미 있으면 그 행의 회원과 마지막 확인 시각을 요청 회원과 현재 시각으로 바꾼다.
	 * 회원의 8·11·17시 알림 설정 중 없는 시각만 기본값으로 만들고 기존 설정은 유지한다.
	 * 플랫폼이 IOS·ANDROID가 아니면 {@code InvalidDevicePlatformException}을 던진다.
	 */
	void registerDeviceInstallation(Long userId, @Valid DeviceInstallationRegisterRequest request);

	/**
	 * 회원 본인의 기기 FID를 지운다. FID가 없거나 다른 회원의 FID가면 아무것도 하지 않는다.
	 */
	void deleteDeviceInstallation(Long userId, @Valid DeviceInstallationUnregisterRequest request);

	/**
	 * 목록의 기기 FID를 회원과 무관하게 모두 지운다. 없는 FID는 무시한다.
	 */
	void deleteDeviceInstallations(@Valid DeviceInstallationDeleteRequest request);

	/**
	 * 회원의 한 시각 알림 수신 여부를 요청 값으로 바꾼다. 회원·시각 행이 있으면 값만 바꾸고, 없으면 새로 만든다.
	 * 시각이 허용 시각(8·11·17시)이 아니면 {@code NotAllowedNotificationHourException}을 던진다.
	 */
	void updatePreference(Long userId, int hour, @Valid NotificationPreferenceUpdateRequest request);

	/**
	 * 회원의 기기 FID와 알림 설정을 모두 지운다.
	 */
	void deleteAllByUserId(Long userId);
}
