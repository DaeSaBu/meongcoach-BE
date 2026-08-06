package com.daesabu.meongcoach.ai.domain.vo;

/**
 * AI 리포트 무료 체험 사용 현황 값 객체.
 * 생성된 리포트 수 하나로 한도·잔여 횟수·발급 가능 여부를 모두 도출한다.
 * MVP라 별도 카운터 없이 리포트 수(count)를 그대로 사용 횟수로 쓴다.
 */
public record AiTrial(int usedCount) {

	// 환경이 아니라 제품 규칙이라 설정이 아닌 도메인 상수로 둔다
	public static final int MAX_COUNT = 3;

	/**
	 * 리포지토리 count 결과로 체험 현황을 만든다. long → int 좁힘을 여기 한 곳에 모은다.
	 */
	public static AiTrial of(long generatedCount) {
		return new AiTrial(Math.toIntExact(generatedCount));
	}

	public int maxCount() {
		return MAX_COUNT;
	}

	/**
	 * 남은 체험 횟수. 발급~생성이 비동기라 한도를 넘겨 저장됐을 수 있으므로 음수가 되지 않게 막는다.
	 */
	public int remainingCount() {
		return Math.max(0, MAX_COUNT - usedCount);
	}

	/**
	 * 체험 횟수가 남아 있는지 판정한다. 소진 시 예외를 던질지 건너뛸지는 호출자가 정한다.
	 */
	public boolean isAvailable() {
		return usedCount < MAX_COUNT;
	}
}
