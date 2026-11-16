package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CurriculumProgressResultRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;

public interface CurriculumProgressRegister {

	/**
	 * 강아지가 그날 훈련한 커리큘럼의 훈련 결과를 성공이나 실패로 기록한다.
	 * 판정을 기다리는 날은 성공이나 실패로, 실패한 날은 성공으로 바꾼다. 지금과 같은 결과를 요청하면 바꾸지 않고 반환한다.
	 * 강아지가 없거나 본인 소유가 아니면 {@code DogNotFoundException}을 던진다.
	 * 결과가 SUCCEEDED, FAILED 중 하나가 아니면 {@code InvalidCurriculumProgressResultException}을 던진다.
	 * 그날 그 커리큘럼을 훈련한 기록이 없으면 {@code CurriculumProgressNotFoundException}을 던진다.
	 * 성공한 날을 실패로 바꾸려 하면 {@code CurriculumProgressAlreadySucceededException}을 던진다.
	 */
	void recordResult(Long userId, Long dogId, Long curriculumId, LocalDate trainedDate,
			@Valid CurriculumProgressResultRequest request);
}
