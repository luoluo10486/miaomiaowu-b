package com.personalblog.ragbackend.rag.service;

import com.personalblog.ragbackend.rag.controller.vo.ConversationMessageVO;
import com.personalblog.ragbackend.rag.enums.ConversationMessageOrder;
import com.personalblog.ragbackend.rag.service.bo.ConversationMessageBO;
import com.personalblog.ragbackend.rag.service.bo.ConversationSummaryBO;

import java.util.List;

/**
 * 会话消息服务接口
 */
public interface ConversationMessageService {
    String addMessage(ConversationMessageBO conversationMessage);

    List<ConversationMessageVO> listMessages(String conversationId, String userId, Integer limit, ConversationMessageOrder order);

    void addMessageSummary(ConversationSummaryBO conversationSummary);
}
