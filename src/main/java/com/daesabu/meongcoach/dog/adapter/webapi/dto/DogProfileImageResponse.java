package com.daesabu.meongcoach.dog.adapter.webapi.dto;

/**
 * 강아지 프로필 이미지 조회 응답. 이미지를 등록하지 않은 강아지는 빈 문자열로 내려간다.
 */
public record DogProfileImageResponse(String profileImageUrl) {

	public static DogProfileImageResponse from(String profileImageUrl) {
		return new DogProfileImageResponse(profileImageUrl);
	}
}
