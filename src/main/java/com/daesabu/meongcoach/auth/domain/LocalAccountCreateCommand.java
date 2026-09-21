package com.daesabu.meongcoach.auth.domain;


public record LocalAccountCreateCommand(Email email, String passwordHash) {
}
