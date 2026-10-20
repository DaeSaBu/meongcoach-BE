package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.MentionTargetType;

/**
 * 멘션 대상 종류 하나의 존재 여부를 확인한다.
 */
public interface MentionTargetVerifier {

	/**
	 * 이 확인기가 담당하는 멘션 대상 종류를 반환한다.
	 */
	MentionTargetType targetType();

	/**
	 * targetId에 해당하는 대상이 있으면 true를 반환한다.
	 */
	boolean exists(Long targetId);
}
