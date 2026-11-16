package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import java.time.LocalDate;

public interface TrainingCalendarFinder {

	/**
	 * 강아지가 시작일부터 종료일까지 훈련한 레슨을 날짜·출처(플랜, 라이브러리)·토픽·커리큘럼별로 묶어 반환한다.
	 * 훈련한 날만 오름차순으로 담고, 토픽·커리큘럼·레슨은 교육 순서를 따른다.
	 * 커리큘럼에는 그날의 진행 상태를, 레슨에는 그날까지 훈련한 날 수를 함께 담는다. 삭제된 레슨의 기록은 빠진다.
	 * 조회 기간과 무관하게 강아지가 처음 훈련한 날을 함께 반환하며, 훈련한 적이 없으면 {@code null}이다.
	 * 강아지가 없거나 본인 소유가 아니면 {@code DogNotFoundException}을 던진다.
	 * 시작일이 종료일보다 늦거나 기간이 62일을 넘으면 {@code InvalidTrainingCalendarPeriodException}을 던진다.
	 */
	TrainingCalendarResult find(Long userId, Long dogId, LocalDate from, LocalDate to);
}
