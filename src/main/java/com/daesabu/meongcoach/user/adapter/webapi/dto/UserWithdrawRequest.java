package com.daesabu.meongcoach.user.adapter.webapi.dto;

/**
 * 탈퇴 요청. Apple 계정 회원만 인가 코드를 실어 보내고 나머지 회원은 본문 없이 호출하므로 필드에 제약을 두지 않는다.
 */
public record UserWithdrawRequest(String appleAuthorizationCode) {
}
