package com.daesabu.meongcoach.notification.domain;

import java.util.Map;

public record PushNotification(String fid, String title, String body, Map<String, String> data, String collapseKey) {
}
