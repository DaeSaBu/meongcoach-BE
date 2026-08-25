package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.DogProfileUpdateCommand;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
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
 * 견종·성별·성격 코드는 여기서 도메인 enum으로 변환하며, 잘못된 코드는 도메인 예외로 400이 된다.
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

	public DogProfileUpdateCommand toCommand() {
		Breed parsedBreed = Breed.from(breed);
		DogSex parsedSex = DogSex.from(sex);
		Set<Personality> parsedPersonalities = Personality.fromCodes(personalities);
		return new DogProfileUpdateCommand(name, parsedBreed, parsedSex, birthDate, weightKg, parsedPersonalities,
				profileImageUrl, expectation);
	}
}
