package com.personalblog.ragbackend.member.service.code.sms;

/**
 * 短信SendReceipt记录类
 */
public record SmsSendReceipt(
        String provider,
        String templateId,
        String requestId,
        boolean debugCodeVisible
) {
}
