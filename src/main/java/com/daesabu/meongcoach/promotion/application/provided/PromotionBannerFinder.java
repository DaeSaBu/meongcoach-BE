package com.daesabu.meongcoach.promotion.application.provided;

import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import java.util.List;

/**
 * 프로모션 배너 조회 공개 API.
 */
public interface PromotionBannerFinder {

	/**
	 * 오늘(한국 날짜) 노출할 배너를 정렬 순서대로 조회한다. 숨긴 배너와 기간 밖의 배너는 빼고, 기간이 비어 있으면 상시 노출한다.
	 */
	List<PromotionBanner> findVisibleBanners();
}
