package com.daesabu.meongcoach.user.domain.command;

import com.daesabu.meongcoach.user.domain.vo.Email;

public record LocalAccountCreateCommand(Email email, String passwordHash) {
}
