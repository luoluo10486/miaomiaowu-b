package com.personalblog.ragbackend.infra.model;

/**
 * 模型调用器接口
 */
@FunctionalInterface
public interface ModelCaller<C, T> {

    T call(C client, ModelTarget target) throws Exception;
}
