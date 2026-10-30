package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;

public record NotificationDeliveryResult(String fid, boolean invalid, NotificationDeliveryException failure) {
}
