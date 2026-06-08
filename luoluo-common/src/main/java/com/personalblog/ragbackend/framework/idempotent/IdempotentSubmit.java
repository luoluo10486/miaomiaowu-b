package com.personalblog.ragbackend.framework.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IdempotentSubmit注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentSubmit {
    String key() default "";

    String message() default "鎮ㄧ殑鎿嶄綔澶揩锛岃绋嶅悗鍐嶈瘯";
}
