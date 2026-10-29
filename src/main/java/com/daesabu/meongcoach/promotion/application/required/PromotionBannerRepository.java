package com.daesabu.meongcoach.promotion.application.required;

import com.daesabu.meongcoach.promotion.domain.PromotionBanner;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PromotionBannerRepository extends JpaRepository<PromotionBanner, Long> {

	@Query("""
			select b
			from PromotionBanner b
			where b.active = true
			  and (b.startsOn is null or b.startsOn <= :today)
			  and (b.endsOn is null or b.endsOn >= :today)
			order by b.sortOrder asc, b.id asc
			""")
	List<PromotionBanner> findVisibleOn(LocalDate today);
}
