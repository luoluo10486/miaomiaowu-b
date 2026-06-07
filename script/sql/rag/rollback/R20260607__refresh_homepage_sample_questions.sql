-- ============================================================
-- Script type: rollback
-- Version: R20260607
-- Module: rag
-- Description: Roll back homepage sample questions to previous seeded copy.
-- ============================================================

update t_sample_question
set title = 'RAG 入门',
    description = '了解 RAG 的基本概念和适用场景',
    question = '什么是 RAG？它和普通大模型问答有什么区别？',
    sort_order = 1,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 1;

update t_sample_question
set title = '文档上传',
    description = '查看知识库文档上传与解析流程',
    question = '知识库文档上传后，系统会如何处理？',
    sort_order = 2,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 2;

update t_sample_question
set title = '向量检索',
    description = '了解检索召回与排序策略',
    question = '向量检索和关键词检索在这个项目里是怎么配合的？',
    sort_order = 3,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 3;
