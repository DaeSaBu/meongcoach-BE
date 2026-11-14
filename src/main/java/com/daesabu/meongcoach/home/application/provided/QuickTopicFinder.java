package com.daesabu.meongcoach.home.application.provided;

import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import java.util.List;

public interface QuickTopicFinder {

	/**
	 * 홈 「바로 훈련해볼까요?」에 보여 줄 토픽을 최대 5개 반환한다. 모든 회원에게 같은 규칙을 쓴다.
	 * 활성 플랜이 있으면 플랜의 토픽만 플랜 순서대로 반환하며 나머지 칸은 채우지 않는다. 진행 중인 토픽은 IN_PROGRESS, 그 밖은 NOT_STARTED다.
	 * 활성 플랜이 없으면 시작하지 않은 토픽만 교육 순서대로 NOT_STARTED로 반환한다.
	 * 활성 플랜의 토픽은 사진(thumbnailUrl) 유무와 관계없이 반환하고, 추천에서는 사진이 없거나 빈 토픽을 제외한다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	List<QuickTopicResult> find(Long userId, Long dogId);
}
