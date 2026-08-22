package com.daesabu.meongcoach.dog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 강아지 프로필 수정 입력. 소유자·선택 상태는 수정 대상이 아니라 담지 않는다.
 */
public record DogProfileUpdateCommand(String name, Breed breed, DogSex sex, LocalDate birthDate,
                                      BigDecimal weightKg, String profileImageUrl, String expectation) {
}
