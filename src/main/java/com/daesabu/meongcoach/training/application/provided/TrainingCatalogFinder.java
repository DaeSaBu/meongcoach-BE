package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;

/**
 * 교육 카탈로그 조회 능력. 맞춤 플랜 구성에 쓰는 전체 교육 목록을 제공한다.
 */
public interface TrainingCatalogFinder {

	/**
	 * 카테고리, 토픽, 커리큘럼, 레슨을 정렬 순서 오름차순으로 조회한다.
	 * 회원이 이용권이 없는 유료 커리큘럼도 목록에 남기고 locked만 true로 표시한다.
	 */
	TrainingCatalogResult findCatalog(Long userId);
}
