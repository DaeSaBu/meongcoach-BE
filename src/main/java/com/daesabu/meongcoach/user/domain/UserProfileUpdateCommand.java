package com.daesabu.meongcoach.user.domain;

import java.time.LocalDate;

public record UserProfileUpdateCommand(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                       String gender) {
}
