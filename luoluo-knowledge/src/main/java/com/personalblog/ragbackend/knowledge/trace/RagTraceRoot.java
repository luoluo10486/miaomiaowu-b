package com.personalblog.ragbackend.knowledge.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RAG追踪Root注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RagTraceRoot {

    String name() default "";

    String conversationIdArg() default "conversationId";

    String taskIdArg() default "taskId";
}
