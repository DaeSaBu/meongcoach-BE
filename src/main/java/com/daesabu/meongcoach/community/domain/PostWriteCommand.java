package com.daesabu.meongcoach.community.domain;

public record PostWriteCommand(String content, MentionCommand mention) {
}
