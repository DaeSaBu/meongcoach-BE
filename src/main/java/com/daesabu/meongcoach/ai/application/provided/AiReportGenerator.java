package com.daesabu.meongcoach.ai.application.provided;

/**
 * 업로드된 영상을 분석해 AI 리포트를 생성한다.
 * 모듈 경계를 넘는 값이라 문자열로 받는다. objectKey는 중복 제거와 소유자 식별에,
 * videoS3Uri는 이벤트가 발생한 버킷 기준의 s3://버킷/키 형태로 영상 분석 요청에 쓴다.
 */
public interface AiReportGenerator {

	void generate(String objectKey, String videoS3Uri);
}
