package com.daesabu.meongcoach.shared.security;

import java.util.Arrays;
import java.util.Locale;

public enum AppPlatform {

	IOS,
	ANDROID,
	;

	public static AppPlatform from(String value) {
		return Arrays.stream(values())
				.filter(platform -> platform.headerValue().equals(value))
				.findFirst()
				.orElseThrow(InvalidAppVersionException::new);
	}

	private String headerValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
