package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.NotificationEventSettingResult;
import java.util.List;

public record NotificationEventSettingsResponse(List<NotificationEventSettingResult> settings) {
}
