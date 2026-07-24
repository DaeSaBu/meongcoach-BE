package com.daesabu.meongcoach.user.domain;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * 이메일 값 객체. 생성 시점에 형식을 검증하므로 인스턴스가 존재하면 항상 유효한 이메일이다.
 */
@Embeddable
public record Email(String address) {

	private static final int MAX_LENGTH = 255;
	private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	public Email {
		if (address == null || address.isBlank()) {
			throw new InvalidEmailException(address);
		}
		if (address.length() > MAX_LENGTH || !FORMAT.matcher(address).matches()) {
			throw new InvalidEmailException(address);
		}
	}
}
