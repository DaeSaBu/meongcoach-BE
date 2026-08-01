package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum TrainingErrorCode implements ErrorCode {

	TRAINING_TOPIC_NOT_FOUND(404, "토픽을 찾을 수 없습니다."),
	TRAINING_CURRICULUM_NOT_FOUND(404, "커리큘럼을 찾을 수 없습니다."),
	TRAINING_LESSON_NOT_FOUND(404, "레슨을 찾을 수 없습니다."),
	TRAINING_TOPIC_NOT_CONFIGURED(404, "등록된 토픽이 없습니다.");

	private final int status;
	private final String message;

	TrainingErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
