package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 등록 입력. 견종·성별·성격 코드의 enum 변환·검증은 요청 DTO(toInfo)가 수행하며,
 * personalities는 null 없이 빈 Set으로 전달한다. profileImageUrl과 expectation은 선택 입력이라 null을 허용한다.
 */
public record DogRegisterInfo(String name, Breed breed, DogSex sex, LocalDate birthDate,
                              BigDecimal weightKg, Set<Personality> personalities, String profileImageUrl,
                              String expectation) {
}
