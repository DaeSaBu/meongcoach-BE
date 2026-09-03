package com.daesabu.meongcoach.user.domain.vo;

import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 리프레시 토큰의 JWT jti 값 객체. 생성 시점에 형식을 검증하므로 인스턴스가 존재하면 항상 UUID 문자열(36자)이다.
 * 새 값은 generate로만 만들고, JWT에서 꺼낸 문자열은 생성자로 감싸 검증한다.
 */
@Embeddable
public record RefreshTokenId(String value) {

	// UUID.fromString은 비정규 형식도 받아들여 36자 컬럼 제약과 어긋나므로 canonical 형식만 허용한다
	private static final Pattern FORMAT = Pattern.compile(
			"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

	public RefreshTokenId {
		if (value == null || !FORMAT.matcher(value).matches()) {
			// 예외 detail은 응답에 노출되므로 값을 담지 않는다
			throw new InvalidRefreshTokenException();
		}
	}

	public static RefreshTokenId generate() {
		return new RefreshTokenId(UUID.randomUUID().toString());
	}
}
