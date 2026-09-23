package com.daesabu.meongcoach.auth.domain;

public record SocialProfile(SocialProvider provider, String providerId, Email email) {
}
