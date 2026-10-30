package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;

public record NotificationSendResult(List<String> invalidFids, int failed, NotificationDeliveryException failure) {
}
