package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DogProfileUpdateCommand(String name, Breed breed, DogSex sex, LocalDate birthDate,
                                      boolean birthDateEstimated, BigDecimal weightKg, Set<Personality> personalities,
                                      String profileImageUrl, String expectation) {
}
