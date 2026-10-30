package com.daesabu.meongcoach.notification.application.provided;

public record NotificationDispatchResult(int fids, int invalidated, int failed, boolean duplicate) {
}
