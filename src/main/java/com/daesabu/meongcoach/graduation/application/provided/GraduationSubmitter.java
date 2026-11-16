package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

public interface GraduationSubmitter {
	/**
	 * 강아지의 첫 제출 또는 보완 후 새 제출을 생성한다. 그 강아지로 검수 대기 중이거나 승인된 토픽은 제출을 거절한다.
	 * 강아지 ID가 없으면 회원이 선택한 강아지로 제출한다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 DogNotFoundException을 던진다.
	 */
	GraduationSubmission submit(Long userId, Long dogId, Long topicId, GraduationSubmissionRequest request);
}
