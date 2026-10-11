package com.daesabu.meongcoach.community.domain;

import java.util.List;

public record PostWriteCommand(Long userId, String content, List<MentionCommand> mentions) {
}
