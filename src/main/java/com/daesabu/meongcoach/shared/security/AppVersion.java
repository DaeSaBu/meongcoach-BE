package com.daesabu.meongcoach.shared.security;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AppVersion(int major, int minor, int patch) {

	private static final Pattern FORMAT = Pattern.compile("(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})");
	private static final Comparator<AppVersion> ORDER = Comparator.comparingInt(AppVersion::major)
			.thenComparingInt(AppVersion::minor)
			.thenComparingInt(AppVersion::patch);

	public static AppVersion from(String value) {
		if (value == null) {
			throw new InvalidAppVersionException();
		}
		Matcher matcher = FORMAT.matcher(value);
		if (!matcher.matches()) {
			throw new InvalidAppVersionException();
		}
		int major = Integer.parseInt(matcher.group(1));
		int minor = Integer.parseInt(matcher.group(2));
		int patch = Integer.parseInt(matcher.group(3));
		return new AppVersion(major, minor, patch);
	}

	public boolean isOlderThan(AppVersion other) {
		return ORDER.compare(this, other) < 0;
	}
}
