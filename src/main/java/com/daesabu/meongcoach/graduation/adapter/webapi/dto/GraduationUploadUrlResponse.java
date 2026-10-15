package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;

public record GraduationUploadUrlResponse(String uploadUrl, String objectKey, long expiresInSeconds) {

	public static GraduationUploadUrlResponse from(VideoUploadUrlResult result) {
		return new GraduationUploadUrlResponse(result.uploadUrl(), result.objectKey(), result.expiresInSeconds());
	}
}
