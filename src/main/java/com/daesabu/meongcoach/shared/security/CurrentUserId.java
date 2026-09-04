package com.daesabu.meongcoach.shared.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 사용자의 ID(JWT sub)를 컨트롤러 파라미터로 주입받기 위한 애노테이션.
 * {@code Long} 타입 파라미터에만 붙일 수 있다.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
