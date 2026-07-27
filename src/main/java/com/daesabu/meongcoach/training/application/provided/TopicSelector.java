package com.daesabu.meongcoach.training.application.provided;

/**
 * 커리큘럼 화면 토픽 선택 능력. 사용자가 커리큘럼 탭에서 볼 토픽을 바꾼다.
 */
public interface TopicSelector {

	/**
	 * 사용자의 커리큘럼 화면을 해당 토픽으로 바꾼다. 같은 토픽을 다시 선택해도 결과는 같다.
	 *
	 * @throws com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException 토픽이 없으면 발생한다
	 */
	void selectTopic(Long userId, Long topicId);
}
