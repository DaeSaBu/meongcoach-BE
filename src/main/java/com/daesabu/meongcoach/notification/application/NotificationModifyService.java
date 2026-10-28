package com.daesabu.meongcoach.notification.application;


import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationModifyService implements NotificationRegister {

	private final DeviceTokenRepository deviceTokenRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;

	@Override
	@Transactional
	public void registerDeviceToken(Long userId, DeviceTokenRegisterRequest request) {
		DeviceTokenRegisterCommand command = request.toCommand();
		Instant now = Instant.now();

		upsertDeviceToken(userId, command, now);
		createDefaultPreferencesIfAbsent(userId);
	}

	private void upsertDeviceToken(Long userId, DeviceTokenRegisterCommand command, Instant now) {
		Optional<DeviceToken> existing = deviceTokenRepository.findByToken(command.token());
		if (existing.isPresent()) {
			existing.get().renew(userId, now);
			return;
		}
		deviceTokenRepository.save(DeviceToken.register(userId, command, now));
	}

	private void createDefaultPreferencesIfAbsent(Long userId) {
		if (!notificationPreferenceRepository.findAllByUserId(userId).isEmpty()) {
			return;
		}
		// 같은 회원이 두 기기에서 동시에 처음 등록하면 한쪽이 유니크 위반으로 실패한다.
		// 같은 트랜잭션 안에서 잡아도 커밋이 실패하므로 그대로 올리고, 앱이 실행마다 다시 등록하므로 다음 요청에서 해소된다.
		notificationPreferenceRepository.saveAll(NotificationPreference.createDefaults(userId));
	}

	@Override
	@Transactional
	public void deleteDeviceToken(Long userId, String token) {
		deviceTokenRepository.deleteByUserIdAndToken(userId, token);
	}

	@Override
	@Transactional
	public void deleteDeviceTokens(DeviceTokenDeleteRequest request) {
		if (request.tokens().isEmpty()) {
			return;
		}
		deviceTokenRepository.deleteAllByTokenIn(request.tokens());
	}

	@Override
	@Transactional
	public void updatePreference(Long userId, int hour, NotificationPreferenceUpdateRequest request) {
		NotificationHour.validateAllowed(hour);
		boolean enabled = request.enabled();

		Optional<NotificationPreference> existing = notificationPreferenceRepository.findByUserIdAndHour(userId, hour);
		if (existing.isPresent()) {
			existing.get().change(enabled);
			return;
		}
		notificationPreferenceRepository.save(NotificationPreference.create(userId, hour, enabled));
	}

	@Override
	@Transactional
	public void deleteAllByUserId(Long userId) {
		deviceTokenRepository.deleteAllByUserId(userId);
		notificationPreferenceRepository.deleteAllByUserId(userId);
	}
}
