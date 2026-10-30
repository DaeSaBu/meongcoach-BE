package com.daesabu.meongcoach.notification.domain;

import java.util.List;

public record NotificationSendResult(List<String> invalidFids, int failed) {
}
