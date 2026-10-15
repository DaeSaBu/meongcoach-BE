package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum LifeStage {
	PUPPY(EntitlementType.PUPPY),
	JUNIOR(EntitlementType.JUNIOR),
	ADULT(EntitlementType.ADULT),
	SENIOR(EntitlementType.SENIOR),
	;

	private static final int JUNIOR_FROM_MONTHS = 12;
	private static final int ADULT_FROM_MONTHS = 24;
	private static final int SENIOR_FROM_MONTHS = 96;
	private static final int MONTHS_PER_YEAR = 12;

	private final EntitlementType entitlementType;

	LifeStage(EntitlementType entitlementType) {
		this.entitlementType = entitlementType;
	}

	public static LifeStage of(Integer ageInMonths) {
		if (ageInMonths == null || ageInMonths < JUNIOR_FROM_MONTHS) {
			return PUPPY;
		}
		if (ageInMonths < ADULT_FROM_MONTHS) {
			return JUNIOR;
		}
		if (ageInMonths < SENIOR_FROM_MONTHS) {
			return ADULT;
		}
		return SENIOR;
	}

	public static String formatAge(Integer ageInMonths, boolean estimated) {
		if (ageInMonths == null) {
			return "";
		}
		String age = describe(ageInMonths);
		if (estimated) {
			return "약 " + age + "(추정)";
		}
		return age;
	}

	public static String ageRange(Integer ageInMonths) {
		if (ageInMonths == null) {
			return "";
		}
		if (ageInMonths < 3) {
			return "3개월 미만";
		}
		if (ageInMonths < 6) {
			return "3~5개월";
		}
		if (ageInMonths < JUNIOR_FROM_MONTHS) {
			return "6~11개월";
		}
		if (ageInMonths < ADULT_FROM_MONTHS) {
			return "1살(12~23개월)";
		}
		if (ageInMonths < 60) {
			return "2~4살(24~59개월)";
		}
		if (ageInMonths < SENIOR_FROM_MONTHS) {
			return "5~7살(60~95개월)";
		}
		return "8살 이상(96개월 이상)";
	}

	public EntitlementType toEntitlementType() {
		return entitlementType;
	}

	public List<LifeStage> priority() {
		List<LifeStage> result = new ArrayList<>();
		result.add(this);
		for (int i = ordinal() - 1; i >= 0; i--) {
			result.add(values()[i]);
		}
		result.addAll(Arrays.asList(values()).subList(ordinal() + 1, values().length));
		return result;
	}

	private static String describe(int ageInMonths) {
		int years = ageInMonths / MONTHS_PER_YEAR;
		int months = ageInMonths % MONTHS_PER_YEAR;
		if (years == 0) {
			return months + "개월";
		}
		if (months == 0) {
			return years + "살";
		}
		return years + "살 " + months + "개월";
	}
}
