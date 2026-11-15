package com.daesabu.meongcoach.training.domain;

import java.time.LocalDate;

public record CurriculumProgressRegisterCommand(
		Long dogId,
		Long curriculumId,
		LocalDate trainedDate) {
}
