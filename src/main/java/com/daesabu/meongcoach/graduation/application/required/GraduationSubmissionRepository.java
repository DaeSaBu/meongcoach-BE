package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {

	Optional<GraduationSubmission> findFirstByGraduationIdOrderByIdDesc(Long graduationId);

	@Query("""
			select s from GraduationSubmission s
			where s.graduation.id in :graduationIds
			and s.id = (select max(latest.id) from GraduationSubmission latest
			            where latest.graduation.id = s.graduation.id)
			""")
	List<GraduationSubmission> findCurrentSubmissionsByGraduationIdIn(List<Long> graduationIds);
}
