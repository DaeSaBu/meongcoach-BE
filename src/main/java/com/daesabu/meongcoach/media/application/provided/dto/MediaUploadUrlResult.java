package com.daesabu.meongcoach.media.application.provided.dto;

public record MediaUploadUrlResult(String uploadUrl, String publicUrl, String objectKey, long expiresInSeconds) {
}
