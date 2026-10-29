package com.daesabu.meongcoach.banner.application.required;

import com.daesabu.meongcoach.banner.domain.Banner;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BannerRepository extends JpaRepository<Banner, Long> {

	@Query("""
			select b
			from Banner b
			where b.active = true
			  and (b.startsAt is null or b.startsAt <= :now)
			  and (b.expiresAt is null or b.expiresAt > :now)
			order by b.sortOrder asc, b.id asc
			""")
	List<Banner> findVisibleAt(Instant now);
}
