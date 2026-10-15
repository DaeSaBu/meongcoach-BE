package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

/**
 * 실습 인증 보상 받기 공개 API.
 */
public interface GraduationRewardClaimer {

	/**
	 * 승인된 수료의 보상으로 전체 이용권 3일을 지급하고 받은 시각과 만료 시각을 기록한다.
	 * 만료 시각은 지금과 이전에 받은 보상의 가장 늦은 만료 시각 가운데 늦은 쪽에서 3일 뒤다.
	 * 이미 받았으면 지급하지 않고 저장된 값을 그대로 돌려준다.
	 * 승인되지 않았거나 제출한 적이 없으면 GraduationNotApprovedException, 결제 대행 지급에 실패하면
	 * EntitlementProviderUnavailableException을 던지고 받지 않은 상태로 남긴다.
	 */
	TopicGraduation claim(Long userId, Long topicId);
}
