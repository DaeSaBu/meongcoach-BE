package com.daesabu.meongcoach.ai.application.required;

/**
 * 영상 분석 연동 지점. 구현은 adapter/client의 Gemini 어댑터가 담당한다.
 */
public interface VideoAnalyzer {

	/**
	 * 주어진 URL의 영상을 분석해 리포트 본문을 돌려준다.
	 */
	String analyze(String videoUrl);
}
