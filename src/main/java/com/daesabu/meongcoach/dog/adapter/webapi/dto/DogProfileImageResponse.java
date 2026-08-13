package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageView;

/**
 * 선택된 강아지의 프로필 이미지 조회 응답. 이미지를 등록하지 않은 강아지는 빈 문자열로 내려간다.
 */
public record DogProfileImageResponse(Long dogId, String profileImageUrl) {

	public static DogProfileImageResponse from(DogProfileImageView view) {
		return new DogProfileImageResponse(view.id(), view.profileImageUrl());
	}
}
