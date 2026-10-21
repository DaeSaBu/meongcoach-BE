package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {
	boolean existsByUserIdAndTopicId(Long userId, Long topicId);

	Optional<GraduationSubmission> findFirstByUserIdAndTopicIdOrderByIdDesc(Long userId, Long topicId);

	@Query("""
			select s
			from GraduationSubmission s
			where s.id in (
				select max(submission.id)
				from GraduationSubmission submission
				where submission.userId = :userId
				group by submission.topicId
			)
			order by s.submittedAt desc nulls last, s.id desc
			""")
	List<GraduationSubmission> findLatestByUserId(Long userId);
}
