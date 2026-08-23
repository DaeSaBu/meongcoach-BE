package com.daesabu.meongcoach.ai.domain;

/**
 * AI 리포트의 진행·결과 상태. 실패도 row로 남겨 클라이언트 폴링에 종료 조건을 제공하기 위한 값이다.
 * 업로드 URL 발급 1건당 row 1개가 UPLOADING으로 생성되고, 업로드 완료 이벤트를 받으면 PENDING으로,
 * 결말에 따라 COMPLETED 또는 FAILED_* 중 하나로 전이한다. 앱은 UPLOADING·PENDING이 아니면 폴링을 멈춘다.
 */
public enum AiReportStatus {

	/** 업로드 URL을 발급했고 업로드 완료 이벤트를 아직 받지 않음 */
	UPLOADING,

	/** 분석 진행 중. 업로드 완료 이벤트를 받은 직후 기록한다 */
	PENDING,

	/** 분석 성공. 이 상태에서만 제목·본문이 채워진다 */
	COMPLETED,

	/** 업로드 URL 유효 시간 안에 업로드 완료 이벤트가 오지 않음. DB에 저장하지 않고 조회 시점에 UPLOADING에서 파생한다 */
	FAILED_UPLOAD,

	/** 컨슈머 처리 시점에 무료 이용 횟수 초과 */
	FAILED_TRIAL_EXCEEDED,

	/** 영상 분석 요청 실패 */
	FAILED_ANALYSIS,

	/** 예측하지 못한 예외로 실패 */
	FAILED_UNEXPECTED,
}
