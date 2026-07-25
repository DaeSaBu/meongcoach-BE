package com.daesabu.meongcoach.user.domain;

// 소셜 제공자가 이메일을 내려주지 않을 수 있으므로 email은 null 허용 String으로 유지한다
public record SocialAccountLinkCommand(SocialProvider provider, String providerId, String email) {
}
