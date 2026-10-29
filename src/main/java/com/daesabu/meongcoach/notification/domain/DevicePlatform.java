package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidDevicePlatformException;
import java.util.Locale;

public enum DevicePlatform {
	IOS,
	ANDROID,
	;

	public static DevicePlatform from(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidDevicePlatformException(value);
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new InvalidDevicePlatformException(value);
		}
	}
}
