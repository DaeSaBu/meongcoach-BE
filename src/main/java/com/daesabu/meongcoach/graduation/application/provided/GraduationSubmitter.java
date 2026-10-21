package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

public interface GraduationSubmitter {
	/**
	 * 첫 제출 또는 보완 후 새 제출을 생성한다. 검수 대기 중이거나 승인된 토픽은 제출을 거절한다.
	 */
	GraduationSubmission submit(Long userId, Long topicId, GraduationSubmissionRequest request);
}
