package com.daesabu.meongcoach.support;

import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@MockitoBean(types = ActiveEntitlementReader.class)
public @interface NonTransactionalApplicationTest {
}
