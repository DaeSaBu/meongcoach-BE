package com.daesabu.meongcoach.dog.domain.shared;

import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;

public enum DogSex {
	MALE,
	FEMALE,
	;

	public static DogSex from(String value) {
		if (value == null) {
			throw new InvalidDogSexException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidDogSexException(value);
		}
	}
}
