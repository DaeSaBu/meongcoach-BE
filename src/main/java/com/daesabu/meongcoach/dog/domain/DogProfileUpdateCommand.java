package com.daesabu.meongcoach.dog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 프로필 수정 입력. 전체 교체 입력이라 성격 집합도 함께 담는다. 소유자·선택 상태는 수정 대상이 아니라 담지 않는다.
 */
public record DogProfileUpdateCommand(String name, Breed breed, DogSex sex, LocalDate birthDate,
                                      BigDecimal weightKg, Set<Personality> personalities,
                                      String profileImageUrl, String expectation) {
}
