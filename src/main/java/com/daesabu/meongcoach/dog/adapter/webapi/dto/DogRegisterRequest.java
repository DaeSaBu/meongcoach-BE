package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 단건 등록 요청. 입력 항목과 검증 규칙은 온보딩 강아지 등록 요청과 같다.
 * 견종·성별·성격 코드의 enum 변환은 Dog가 수행하며, 잘못된 코드는 도메인 예외로 400이 된다.
 */
public record DogRegisterRequest(
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(max = 30) String breed,
		@NotBlank String sex,
		@Past LocalDate birthDate,
		@NotNull @Positive BigDecimal weightKg,
		Set<String> personalities,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 500) String expectation) {

	public DogRegisterCommand toCommand() {
		return new DogRegisterCommand(name, breed, sex, birthDate, weightKg, personalities, profileImageUrl,
				expectation);
	}
}
