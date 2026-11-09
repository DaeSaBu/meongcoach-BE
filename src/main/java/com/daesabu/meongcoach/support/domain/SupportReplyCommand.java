package com.daesabu.meongcoach.support.domain;

import java.util.List;

public record SupportReplyCommand(String event, String type, String chatType, String senderType, String text,
                                  List<String> options, boolean broadcast, boolean marketing, String memberId) {
}
