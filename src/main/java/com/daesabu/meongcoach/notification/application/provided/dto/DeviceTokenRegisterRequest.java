package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegisterRequest(
		@NotBlank @Size(max = 512) String token,
		@NotBlank String platform) {

	public DeviceTokenRegisterCommand toCommand() {
		return new DeviceTokenRegisterCommand(token, DevicePlatform.from(platform));
	}
}
