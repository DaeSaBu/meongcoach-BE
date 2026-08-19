package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 토큰 발급 방식. OAuth2 token endpoint의 grant_type과 같은 역할을 한다.
 */
public enum GrantType {
	SOCIAL,
	REFRESH,
	;

	// 알 수 없는 값은 역직렬화 실패 대신 null로 두어 grantType의 @NotNull 검증 응답으로 수렴시킨다
	@JsonCreator
	public static GrantType from(String value) {
		for (GrantType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}
