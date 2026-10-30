package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;

public record NotificationTargetResult(Long userId, String fid, DevicePlatform platform) {

	public static NotificationTargetResult from(DeviceInstallation deviceInstallation) {
		return new NotificationTargetResult(deviceInstallation.getUserId(), deviceInstallation.getFid(), deviceInstallation.getPlatform());
	}
}
