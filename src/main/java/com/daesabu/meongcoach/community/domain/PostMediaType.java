package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostMediaTypeException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum PostMediaType {
	IMAGE("사진", 3),
	VIDEO("영상", 2);

	private final String displayName;
	private final int maxCount;

	PostMediaType(String displayName, int maxCount) {
		this.displayName = displayName;
		this.maxCount = maxCount;
	}

	public static PostMediaType from(String code) {
		return Arrays.stream(values())
				.filter(type -> type.name().equalsIgnoreCase(code))
				.findFirst()
				.orElseThrow(() -> new InvalidPostMediaTypeException(code));
	}
}
