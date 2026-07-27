package com.daesabu.meongcoach.shared.webapi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인 사용자 식별자를 컨트롤러 파라미터에 바인딩한다. 컨트롤러가 인증 방식에 직접 묶이지 않도록, 사용자 식별은 {@link LoginUserArgumentResolver} 한 곳에서만
 * 해석한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginUser {
}
