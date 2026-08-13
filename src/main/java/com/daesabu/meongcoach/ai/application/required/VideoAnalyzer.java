package com.daesabu.meongcoach.ai.application.required;

/**
 * 영상 분석 연동 지점. 구현은 adapter/client의 어댑터가 담당한다.
 */
public interface VideoAnalyzer {

	/**
	 * 주어진 URL(비공개 버킷은 presigned GET URL)의 영상을 분석해 recommend/report/solution 구조로
	 * 검증된 리포트 JSON 문자열을 돌려준다. 분석에 실패하거나 응답이 구조에 맞지 않으면 예외를 던진다.
	 */
	String analyze(String videoUrl);
}
