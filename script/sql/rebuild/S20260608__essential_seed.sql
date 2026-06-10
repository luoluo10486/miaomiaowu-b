-- ============================================================
-- Script type: seed
-- Version: S20260608
-- Scope: rebuild
-- Description: Keep only essential seed data after schema rebuild.
-- Notes:
--   1. Preserve the current three homepage sample questions exported from the live database.
--   2. Preserve the global daily question quota configuration.
-- ============================================================

insert into t_rag_question_quota_config (
    config_key,
    daily_limit,
    remark,
    deleted,
    create_time,
    update_time
)
select 'global_daily_question_limit',
       5,
       '全局每日提问上限',
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_rag_question_quota_config
    where config_key = 'global_daily_question_limit'
      and deleted = 0
);

insert into t_sample_question (
    id,
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
       1,
       'Java 知识文档',
       '适合查看 Java、Spring Boot 和项目实现细节',
       'Bean的生命周期？',
       1,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where id = 1
       or question = 'Bean的生命周期？'
);

insert into t_sample_question (
    id,
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
select 2,
       1,
       '今日天气查询',
       '查询当地今天的真实天气，走 MCP 天气工具',
       '广州今天的天气怎么样？',
       2,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where id = 2
       or question = '广州今天的天气怎么样？'
);

insert into t_sample_question (
    id,
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
select 3,
       1,
       '聊天记录检索',
       '可询问聊天记录相关内容，查询前请先联系管理员开通权限',
       'xxx什么时候提起过xxx?',
       3,
       1,
       0,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from t_sample_question
    where id = 3
       or question = 'xxx什么时候提起过xxx?'
);
