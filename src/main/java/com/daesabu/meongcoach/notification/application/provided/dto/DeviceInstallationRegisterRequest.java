package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceInstallationRegisterRequest(
		@NotBlank @Size(max = 512) String fid,
		@NotBlank String platform) {

	public DeviceInstallationRegisterCommand toCommand() {
		return new DeviceInstallationRegisterCommand(fid, DevicePlatform.from(platform));
	}
}
