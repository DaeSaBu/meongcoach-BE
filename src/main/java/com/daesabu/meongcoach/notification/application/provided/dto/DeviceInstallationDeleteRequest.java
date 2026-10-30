package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DeviceInstallationDeleteRequest(
		@NotNull @Size(max = 1000) List<@NotBlank @Size(max = 512) String> fids) {
}
