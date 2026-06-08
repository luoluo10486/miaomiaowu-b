package com.personalblog.ragbackend.common.captcha.dto;

/**
 * Image验证码响应对象
 */
public record ImageCaptchaResponse(
        String captchaKey,
        String imageBase64,
        long expiresIn
) {
}
