package com.marmot.zk.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.marmot.zk.enums.EnumMethodAuthType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface BeforeMethod {

    EnumMethodAuthType authType() default EnumMethodAuthType.NOAUTH;
}

