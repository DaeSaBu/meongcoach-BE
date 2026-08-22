package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdateInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 프로필 전체 교체 요청. 검증 규칙은 온보딩 강아지 등록 요청과 같다.
 */
public record DogProfileUpdateRequest(
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(max = 30) String breed,
		@NotBlank String sex,
		@Past LocalDate birthDate,
		@NotNull @Positive BigDecimal weightKg,
		Set<String> personalities,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 500) String expectation) {

	public DogProfileUpdateInfo toInfo() {
		return new DogProfileUpdateInfo(name, breed, sex, birthDate, weightKg, personalities, profileImageUrl,
				expectation);
	}
}
