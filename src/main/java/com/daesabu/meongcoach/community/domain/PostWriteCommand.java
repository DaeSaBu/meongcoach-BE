package com.daesabu.meongcoach.community.domain;

import java.util.List;

public record PostWriteCommand(String content, MentionCommand mention, List<PostMediaCommand> media) {
}
