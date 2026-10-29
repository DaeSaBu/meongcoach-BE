package com.daesabu.meongcoach.banner.application.required;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BannerRepository extends JpaRepository<Banner, Long> {

	/**
	 * 주어진 시각이 시작 이상·만료 미만인 활성 배너를 정렬 순서와 ID 오름차순으로 조회한다. 시작·만료 시각이 없으면 해당 제한을 적용하지 않는다.
	 */
	@Query("""
			select b
			from Banner b
			where b.active = true
			  and (b.startsAt is null or b.startsAt <= :now)
			  and (b.expiresAt is null or b.expiresAt > :now)
			order by b.sortOrder asc, b.id asc
			""")
	List<Banner> findActiveBannersAt(Instant now);
}
