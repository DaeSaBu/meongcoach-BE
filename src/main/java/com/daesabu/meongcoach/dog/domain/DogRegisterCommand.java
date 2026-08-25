package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DogRegisterCommand(Long userId, String name, Breed breed, DogSex sex, LocalDate birthDate,
                                 BigDecimal weightKg, String profileImageUrl, String expectation) {
}
