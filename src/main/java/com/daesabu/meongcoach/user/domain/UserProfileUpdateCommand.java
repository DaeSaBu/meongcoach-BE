package com.daesabu.meongcoach.user.domain;

public record UserProfileUpdateCommand(String nickname, String profileImageUrl, String mbti) {
}
