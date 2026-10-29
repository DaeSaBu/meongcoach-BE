package com.daesabu.meongcoach.banner.application.provided;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.util.List;

/**
 * 배너 조회 공개 API.
 */
public interface BannerFinder {

	/**
	 * 현재 시각이 시작·만료 범위 안인 활성 배너를 정렬 순서대로 조회한다. 시작·만료 시각이 없으면 해당 제한을 적용하지 않는다.
	 */
	List<Banner> findActiveBanners();
}
