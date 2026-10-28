package com.daesabu.meongcoach.partner.application.provided;

import com.daesabu.meongcoach.partner.domain.PartnerBanner;
import java.util.List;

/**
 * 제휴 배너 조회 공개 API.
 */
public interface PartnerBannerFinder {

	/**
	 * 오늘(한국 날짜) 노출할 배너를 정렬 순서대로 조회한다. 숨긴 배너와 기간 밖의 배너는 빼고, 기간이 비어 있으면 상시 노출한다.
	 */
	List<PartnerBanner> findVisibleBanners();
}
