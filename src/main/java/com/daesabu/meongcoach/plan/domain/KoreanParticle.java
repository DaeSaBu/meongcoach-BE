package com.daesabu.meongcoach.plan.domain;

public final class KoreanParticle {

	private static final char HANGUL_START = '가';
	private static final char HANGUL_END = '힣';
	private static final int FINAL_CONSONANT_CYCLE = 28;
	private static final String UNKNOWN = "을(를)";

	private KoreanParticle() {
	}

	public static String objectOf(String word) {
		if (word == null || word.isBlank()) {
			return UNKNOWN;
		}
		char last = word.strip().charAt(word.strip().length() - 1);
		if (last < HANGUL_START || last > HANGUL_END) {
			return UNKNOWN;
		}
		if ((last - HANGUL_START) % FINAL_CONSONANT_CYCLE == 0) {
			return "를";
		}
		return "을";
	}
}
