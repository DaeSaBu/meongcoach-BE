package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 교육 카테고리 조회 능력. 라이브러리 탭 화면을 구성한다.
 */
public interface TrainingCategoryFinder {

	/**
	 * 전체 교육 카테고리를 정렬 순서 오름차순으로 조회한다. 각 카테고리의 토픽도 정렬 순서 오름차순이다.
	 */
	List<TrainingCategoryView> findAll();
}
