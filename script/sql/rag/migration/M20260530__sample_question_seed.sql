-- ============================================================
-- Script type: migration
-- Version: M20260530
-- Module: rag
-- Description: Seed sample questions for t_sample_question.
-- ============================================================

insert into t_sample_question (
    kb_id,
    title,
    description,
    question,
    sort_order,
    enabled,
    deleted,
    create_time,
    update_time
)
select 1,
       'RAG 入门',
       '了解 RAG 的基本概念和适用场景',
       '什么是 RAG？它和普通大模型问答有什么区别？',
       1,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where question = '什么是 RAG？它和普通大模型问答有什么区别？'
      and deleted = 0
);

insert into t_sample_question (
    kb_id,
    title,
    description,
    question,
    sort_order,
    enabled,
    deleted,
    create_time,
    update_time
)
select 1,
       '文档上传',
       '查看知识库文档上传与解析流程',
       '知识库文档上传后，系统会如何处理？',
       2,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where question = '知识库文档上传后，系统会如何处理？'
      and deleted = 0
);

insert into t_sample_question (
    kb_id,
    title,
    description,
    question,
    sort_order,
    enabled,
    deleted,
    create_time,
    update_time
)
select 1,
       '向量检索',
       '了解检索召回与排序策略',
       '向量检索和关键词检索在这个项目里是怎么配合的？',
       3,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where question = '向量检索和关键词检索在这个项目里是怎么配合的？'
      and deleted = 0
);

insert into t_sample_question (
    kb_id,
    title,
    description,
    question,
    sort_order,
    enabled,
    deleted,
    create_time,
    update_time
)
select 1,
       '会话记忆',
       '查看多轮对话与记忆摘要机制',
       '多轮对话的上下文是如何保存和恢复的？',
       4,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where question = '多轮对话的上下文是如何保存和恢复的？'
      and deleted = 0
);

insert into t_sample_question (
    kb_id,
    title,
    description,
    question,
    sort_order,
    enabled,
    deleted,
    create_time,
    update_time
)
select 1,
       '接口调试',
       '查看接口联调和错误排查方式',
       '如果 RAG 接口返回 500，应该先检查哪些配置和表？',
       5,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where question = '如果 RAG 接口返回 500，应该先检查哪些配置和表？'
      and deleted = 0
);
