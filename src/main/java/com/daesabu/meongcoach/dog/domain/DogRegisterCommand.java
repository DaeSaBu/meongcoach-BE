package com.daesabu.meongcoach.dog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DogRegisterCommand(Long userId, String name, Breed breed, DogSex sex, LocalDate birthDate,
                                 BigDecimal weightKg) {
}
