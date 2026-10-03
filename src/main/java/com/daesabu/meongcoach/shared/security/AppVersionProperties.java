package com.daesabu.meongcoach.shared.security;

import jakarta.validation.constraints.AssertTrue;
import java.util.Arrays;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.app-version")
public record AppVersionProperties(Map<AppPlatform, AppVersion> minimum) {

	@AssertTrue(message = "모든 플랫폼의 최소 지원 앱 버전이 필요합니다")
	public boolean isMinimumForEveryPlatform() {
		if (minimum == null) {
			return false;
		}
		return Arrays.stream(AppPlatform.values())
				.allMatch(minimum::containsKey);
	}

	public void verify(String platformHeader, String versionHeader) {
		if (isBlank(platformHeader) || isBlank(versionHeader)) {
			throw new AppUpdateRequiredException();
		}
		AppPlatform platform = AppPlatform.from(platformHeader);
		AppVersion version = AppVersion.from(versionHeader);
		if (version.isOlderThan(minimum.get(platform))) {
			throw new AppUpdateRequiredException();
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
