package com.daesabu.meongcoach.user.domain.command;

import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.shared.Email;

// 소셜 제공자가 이메일을 내려주지 않을 수 있으므로 email은 null을 허용한다 (Email.ofNullable)
public record SocialAccountLinkCommand(SocialProvider provider, String providerId, Email email) {
}
