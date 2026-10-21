package com.daesabu.meongcoach.graduation.domain;

import java.time.Instant;
import java.util.List;

public record GraduationRevisionCommand(String note, List<String> retakeInstructions,
                                        String retakeImageUrl, Instant reviewedAt) {

	public GraduationRevisionCommand {
		retakeInstructions = List.copyOf(retakeInstructions);
	}
}
