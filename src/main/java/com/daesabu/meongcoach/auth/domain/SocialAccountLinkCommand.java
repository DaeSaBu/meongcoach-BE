package com.daesabu.meongcoach.auth.domain;


public record SocialAccountLinkCommand(SocialProvider provider, String providerId, Email email) {
}
