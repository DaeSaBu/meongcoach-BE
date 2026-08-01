package com.daesabu.meongcoach.ai.application.provided;

/**
 * 업로드된 영상을 분석해 AI 리포트를 생성한다.
 * 모듈 경계를 넘는 값이라 영상 객체 키를 문자열로 받는다.
 */
public interface AiReportGenerator {

	void generate(String objectKey);
}
