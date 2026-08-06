package com.daesabu.meongcoach.ai.application.provided;

/**
 * AI 리포트 무료 체험 사용 현황.
 *
 * @param usedCount      지금까지 생성한 리포트 수
 * @param maxCount       무료 체험 최대 횟수
 * @param remainingCount 남은 횟수. 소진했으면 0
 */
public record AiTrialView(int usedCount, int maxCount, int remainingCount) {
}
