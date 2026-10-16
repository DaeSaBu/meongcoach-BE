package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {
}
