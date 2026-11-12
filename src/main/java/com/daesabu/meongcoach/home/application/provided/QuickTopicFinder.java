package com.daesabu.meongcoach.home.application.provided;

import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import java.util.List;

public interface QuickTopicFinder {

	/**
	 * 홈 「바로 훈련해볼까요?」에 보여 줄 토픽을 최대 5개 반환한다. 모든 회원에게 같은 규칙을 쓴다.
	 * 활성 플랜이 있으면 플랜의 토픽만 플랜 순서대로 반환하며 나머지 칸은 채우지 않는다. 진행 중인 토픽은 IN_PROGRESS, 그 밖은 NOT_STARTED다.
	 * 활성 플랜이 없으면 가장 최근에 학습한 진행 중 토픽 하나를 IN_PROGRESS로 둔다.
	 * 이때 나머지는 시작하지 않은 토픽을 회원과 서울 기준 날짜로 정한 순서로 섞어 NOT_STARTED로 채운다.
	 * 강아지가 설정한 개월 수(기본 24) 이상이면 플랜 밖 토픽 중 카테고리 정렬 값이 설정한 값(기본 3, prod 주니어) 미만인 토픽을 반환하지 않는다.
	 * 생일이 없으면 그 개월 수 미만으로 본다.
	 * 사진(thumbnailUrl)이 없거나 빈 토픽은 어느 칸에도 반환하지 않는다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	List<QuickTopicResult> find(Long userId, Long dogId);
}
