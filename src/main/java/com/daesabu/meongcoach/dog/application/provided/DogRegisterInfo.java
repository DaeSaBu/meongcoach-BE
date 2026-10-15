package com.daesabu.meongcoach.dog.application.provided;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DogRegisterInfo(String name, String breed, String sex, LocalDate birthDate,
                              Boolean birthDateEstimated, BigDecimal weightKg, Set<String> personalities, String profileImageUrl,
                              String expectation) {
}
