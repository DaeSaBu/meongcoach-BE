package com.daesabu.meongcoach.training.application.provided;

/**
 * 토픽 학습 완료 판정 공개 API. 완료 여부는 저장하지 않고 요청마다 레슨 진도로 계산한다.
 */
public interface TopicCompletionChecker {

	/**
	 * 토픽의 커리큘럼이 1개 이상이고 모든 커리큘럼의 레슨을 완료했으면 참이다.
	 * 토픽이 없으면 TopicNotFoundException을 던진다.
	 */
	boolean isCompleted(Long userId, Long topicId);
}
