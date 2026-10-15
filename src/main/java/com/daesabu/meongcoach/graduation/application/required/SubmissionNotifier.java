package com.daesabu.meongcoach.graduation.application.required;

/**
 * 실습 인증 제출을 운영자에게 알리는 포트.
 */
public interface SubmissionNotifier {

	/**
	 * 제출 한 건을 운영자 채널에 알린다. 알림 대상이 설정되지 않았거나 전송에 실패해도 예외를 던지지 않는다.
	 */
	void notifySubmitted(SubmissionNotice notice);
}
