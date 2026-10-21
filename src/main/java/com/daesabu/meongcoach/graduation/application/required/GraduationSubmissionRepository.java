package com.daesabu.meongcoach.graduation.application.required;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {
 Optional<GraduationSubmission> findFirstByUserIdAndTopicIdOrderByIdDesc(Long userId, Long topicId);
 @Query("""
  select s from GraduationSubmission s where s.userId = :userId
  and s.id = (select max(latest.id) from GraduationSubmission latest
  where latest.userId = s.userId and latest.topicId = s.topicId)
  order by s.submittedAt desc, s.id desc
  """)
 List<GraduationSubmission> findLatestByUserId(Long userId);
}
