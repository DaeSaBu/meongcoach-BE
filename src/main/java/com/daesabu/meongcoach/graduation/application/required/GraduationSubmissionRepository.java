package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {

	Optional<GraduationSubmission> findByGraduationIdAndAttemptNo(Long graduationId, int attemptNo);

	@Query("""
			select s from GraduationSubmission s
			join s.graduation g
			where g.id in :graduationIds and s.attemptNo = g.attemptNo
			""")
	List<GraduationSubmission> findCurrentSubmissionsByGraduationIdIn(List<Long> graduationIds);
}
