package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// 재발급 요청의 jti로 저장된 토큰을 찾는다. token_id는 유니크 제약이라 최대 한 건이다
	Optional<RefreshToken> findByTokenId(RefreshTokenId tokenId);
}
