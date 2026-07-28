package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 온보딩 완료 여부는 별도 플래그 없이 프로필 행 존재 여부로 판단하므로
 * 상속받은 existsById(userId)만 사용한다.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
