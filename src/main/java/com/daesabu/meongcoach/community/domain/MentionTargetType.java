package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidMentionTargetTypeException;
import java.util.Arrays;

public enum MentionTargetType {
	CARD;

	public static MentionTargetType from(String code) {
		return Arrays.stream(values())
				.filter(type -> type.name().equalsIgnoreCase(code))
				.findFirst()
				.orElseThrow(() -> new InvalidMentionTargetTypeException(code));
	}
}
