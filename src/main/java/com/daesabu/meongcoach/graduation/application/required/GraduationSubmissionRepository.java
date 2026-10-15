package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {

	Optional<GraduationSubmission> findTopByGraduationIdOrderByAttemptNoDesc(Long graduationId);

	List<GraduationSubmission> findAllByGraduationIdIn(Collection<Long> graduationIds);

	List<GraduationSubmission> findAllByResultOrderBySubmittedAtAscIdAsc(SubmissionResult result);
}
