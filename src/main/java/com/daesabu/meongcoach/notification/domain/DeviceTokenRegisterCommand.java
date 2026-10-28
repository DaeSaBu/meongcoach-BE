package com.daesabu.meongcoach.notification.domain;

public record DeviceTokenRegisterCommand(String token, DevicePlatform platform) {
}
