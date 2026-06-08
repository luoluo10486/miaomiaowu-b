package com.personalblog.ragbackend.infra.token;

/**
 * 令牌计数器服务接口
 */
public interface TokenCounterService {

    Integer countTokens(String text);
}
