package com.daesabu.meongcoach.user.domain.command;

import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.shared.Email;

public record SocialAccountLinkCommand(SocialProvider provider, String providerId, Email email) {
}
