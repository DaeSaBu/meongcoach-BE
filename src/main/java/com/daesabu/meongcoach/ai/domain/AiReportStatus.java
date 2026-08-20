package com.daesabu.meongcoach.ai.domain;

/**
 * AI 리포트의 분석 결과 상태. 실패도 row로 남겨 클라이언트 폴링에 종료 조건을 제공하기 위한 값이다.
 * 현재는 성공 저장 경로만 존재해 COMPLETED만 기록되며, 나머지 값은 후속 작업에서 기록을 시작한다.
 */
public enum AiReportStatus {

	/** 분석 진행 중. SQS 컨슈머가 메시지 수신 직후 기록할 예정 (아직 미기록) */
	PENDING,

	/** 분석 성공 */
	COMPLETED,

	/** 컨슈머 처리 시점에 무료 이용 횟수 초과 (아직 미기록) */
	FAILED_TRIAL_EXCEEDED,

	/** 영상 분석 요청 실패 (아직 미기록) */
	FAILED_ANALYSIS,

	/** 예측하지 못한 예외로 실패 (아직 미기록) */
	FAILED_UNEXPECTED,
}
