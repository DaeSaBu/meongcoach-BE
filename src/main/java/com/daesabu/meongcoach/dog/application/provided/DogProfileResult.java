package com.daesabu.meongcoach.dog.application.provided;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DogProfileResult(Long id, String name, LocalDate birthDate, boolean birthDateEstimated,
                               Integer ageInMonths, String breedLabel, String sex, List<String> personalityLabels,
                               BigDecimal weightKg, String expectation) {
}
