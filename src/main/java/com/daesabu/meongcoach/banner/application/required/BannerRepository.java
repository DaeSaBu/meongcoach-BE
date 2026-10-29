package com.daesabu.meongcoach.banner.application.required;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BannerRepository extends JpaRepository<Banner, Long> {

	@Query("""
			select b
			from Banner b
			where b.active = true
			  and (b.startsOn is null or b.startsOn <= :today)
			  and (b.endsOn is null or b.endsOn >= :today)
			order by b.sortOrder asc, b.id asc
			""")
	List<Banner> findVisibleOn(LocalDate today);
}
