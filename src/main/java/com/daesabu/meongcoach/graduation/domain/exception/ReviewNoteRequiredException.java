package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ReviewNoteRequiredException extends DomainException {

	public ReviewNoteRequiredException() {
		super(GraduationErrorCode.GRADUATION_REVIEW_NOTE_REQUIRED);
	}
}
