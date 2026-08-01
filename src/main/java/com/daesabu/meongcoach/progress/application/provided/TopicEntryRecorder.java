package com.daesabu.meongcoach.progress.application.provided;

/**
 * 토픽 진입 기록 능력. 다른 모듈은 이 인터페이스로만 진입을 기록한다.
 */
public interface TopicEntryRecorder {

	/**
	 * 사용자가 토픽에 진입했음을 기록한다. 기록이 없으면 새로 만들고, 있으면 수정 시각만 갱신한다.
	 */
	void enterTopic(Long userId, Long topicId);
}
