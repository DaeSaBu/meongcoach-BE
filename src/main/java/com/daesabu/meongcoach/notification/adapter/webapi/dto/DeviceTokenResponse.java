package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.DeviceTokenResult;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;

public record DeviceTokenResponse(String token, DevicePlatform platform) {

	public static DeviceTokenResponse from(DeviceTokenResult result) {
		return new DeviceTokenResponse(result.token(), result.platform());
	}
}
