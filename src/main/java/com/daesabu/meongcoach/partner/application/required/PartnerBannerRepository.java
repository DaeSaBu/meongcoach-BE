package com.daesabu.meongcoach.partner.application.required;

import com.daesabu.meongcoach.partner.domain.PartnerBanner;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PartnerBannerRepository extends JpaRepository<PartnerBanner, Long> {

	@Query("""
			select b
			from PartnerBanner b
			where b.active = true
			  and (b.startsOn is null or b.startsOn <= :today)
			  and (b.endsOn is null or b.endsOn >= :today)
			order by b.sortOrder asc, b.id asc
			""")
	List<PartnerBanner> findVisibleOn(LocalDate today);
}
