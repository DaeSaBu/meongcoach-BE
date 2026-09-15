package com.daesabu.meongcoach.loadtest;

/**
 * 준비 단계를 마친 계정. 액세스 토큰은 여러 스레드가 공유해도 되지만 리프레시 토큰은 rotation 때문에 1회용이다.
 * reportId는 AI 리포트 폴링 시나리오에서만 채워지고 그 외에는 0이다.
 */
public record PreparedAccount(String email, String password, String accessToken, String refreshToken, long reportId) {

	public static final String CSV_HEADER = "email,accessToken,refreshToken,reportId";

	// 토큰은 base64url 문자만 담고 이메일에도 쉼표가 없어 따옴표 처리 없이 그대로 잇는다
	public String toCsvLine() {
		return email + "," + accessToken + "," + refreshToken + "," + reportId;
	}
}
