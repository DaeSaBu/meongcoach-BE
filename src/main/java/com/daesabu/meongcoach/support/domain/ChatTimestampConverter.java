package com.daesabu.meongcoach.support.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Converter
public class ChatTimestampConverter implements AttributeConverter<LocalDateTime, Instant> {
	@Override
	public Instant convertToDatabaseColumn(LocalDateTime value) {
		return toInstant(value);
	}

	@Override
	public LocalDateTime convertToEntityAttribute(Instant value) {
		return toLocalDateTime(value);
	}

	public static Instant toInstant(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atZone(ZoneId.systemDefault()).toInstant();
	}

	public static LocalDateTime toLocalDateTime(Instant value) {
		if (value == null) {
			return null;
		}
		return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
	}
}
