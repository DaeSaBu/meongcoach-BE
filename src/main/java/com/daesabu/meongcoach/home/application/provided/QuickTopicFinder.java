package com.daesabu.meongcoach.home.application.provided;

import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import java.util.List;

public interface QuickTopicFinder {

	/**
	 * 홈 「바로 훈련해볼까요?」에 보여 줄 토픽을 최대 5개 반환한다.
	 * 회원이 완료한 레슨이 없으면 강아지 나이(두 살 전·후)별 고정 토픽을 STARTER로 반환한다. 생일이 없으면 두 살 전으로 본다.
	 * 완료한 레슨이 있으면 첫 칸에 진행 중인 토픽 하나를 CONTINUE로 두고(활성 플랜 밖에서 가장 최근에 학습한 토픽, 없으면 활성 플랜 안의 토픽),
	 * 나머지는 시작하지 않았고 활성 플랜에 없는 토픽을 회원과 서울 기준 날짜로 정한 순서로 섞어 NEW로 채운다.
	 * 두 살 이후 강아지에게는 훈련 기본기·퍼피 교육 카테고리의 토픽을 반환하지 않는다.
	 * 사진(thumbnailUrl)이 없거나 빈 토픽은 어느 칸에도 반환하지 않는다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	List<QuickTopicResult> find(Long userId, Long dogId);
}
