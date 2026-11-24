package com.daesabu.meongcoach.report.domain;

import com.daesabu.meongcoach.report.domain.exception.InvalidReportReasonException;
import java.util.Arrays;

public enum ReportReason {
	SPAM,
	ABUSE,
	SEXUAL,
	ANIMAL_CRUELTY,
	PRIVACY,
	MISINFORMATION,
	OTHER;

	public static ReportReason from(String code) {
		return Arrays.stream(values())
				.filter(reason -> reason.name().equalsIgnoreCase(code))
				.findFirst()
				.orElseThrow(() -> new InvalidReportReasonException(code));
	}
}
