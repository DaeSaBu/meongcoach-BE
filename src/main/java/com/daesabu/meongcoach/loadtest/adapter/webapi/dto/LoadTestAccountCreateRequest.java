package com.daesabu.meongcoach.loadtest.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.LocalAccountRegisterInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이메일 형식 검증은 user 모듈의 Email 값 객체 한 곳에서만 한다. 비밀번호 상한 72는 BCrypt 입력 길이 제한이다
public record LoadTestAccountCreateRequest(
		@NotBlank String email,
		@NotBlank @Size(min = 8, max = 72) String password) {

	public LocalAccountRegisterInfo toInfo() {
		return new LocalAccountRegisterInfo(email, password);
	}
}
