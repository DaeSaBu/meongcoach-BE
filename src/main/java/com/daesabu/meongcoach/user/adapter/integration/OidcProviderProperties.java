package com.daesabu.meongcoach.user.adapter.integration;

import java.util.List;

/**
 * OIDC 제공자 하나를 검증하는 데 필요한 최소 설정. 제공자별 {@code ~Properties} record가 구현한다.
 * id_token은 제공자가 달라도 "JWKS 서명 + iss + exp + aud" 검증 형태가 같아 설정도 이 세 값으로 충분하다.
 */
interface OidcProviderProperties {

	String issuer();

	String jwkSetUri();

	List<String> audiences();
}
