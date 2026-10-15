package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {

	Optional<GraduationSubmission> findTopByGraduationIdOrderByAttemptNoDesc(Long graduationId);

	List<GraduationSubmission> findAllByGraduationIdIn(Collection<Long> graduationIds);

	List<GraduationSubmission> findAllByResultOrderBySubmittedAtAscIdAsc(SubmissionResult result);

	/**
	 * 제출 엔티티를 영속성 컨텍스트에 올리지 않고 수료 행 id만 읽는다. 수료 행을 먼저 잠근 뒤 제출을 읽기 위해서다.
	 */
	@Query("select s.graduationId from GraduationSubmission s where s.id = :id")
	Optional<Long> findGraduationIdById(Long id);
}
