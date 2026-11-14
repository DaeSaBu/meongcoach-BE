package com.daesabu.meongcoach.ai.application.provided;

public interface PromptTemplateLoader {

	/**
	 * 클래스패스의 프롬프트 템플릿을 읽어 앞뒤 공백을 뺀 내용을 반환한다.
	 * 템플릿의 {{include:경로}} 지시문은 prompts/경로.md 조각의 내용으로 바꾼다.
	 * 템플릿이나 조각을 읽을 수 없거나 비어 있으면, 또는 조각 안에 지시문이 있으면 IllegalStateException을 던진다.
	 */
	String load(String location);
}
