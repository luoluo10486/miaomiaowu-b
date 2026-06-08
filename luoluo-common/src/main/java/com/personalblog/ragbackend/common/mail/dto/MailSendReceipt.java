package com.personalblog.ragbackend.common.mail.dto;

/**
 * 邮件SendReceipt记录类
 */
public record MailSendReceipt(
        String provider,
        String requestId,
        boolean debugPayloadVisible
) {
}
