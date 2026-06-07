-- ============================================================
-- Script type: migration
-- Version: M20260607
-- Module: rag
-- Description: Refresh homepage sample questions for chat welcome screen.
-- ============================================================

update t_sample_question
set title = 'Java 知识文档',
    description = '适合查询 Java、Spring Boot 和项目实现细节',
    question = '请结合知识库说明这个项目里的 Java 权限校验链路是怎么实现的？',
    sort_order = 1,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 1;

update t_sample_question
set title = '微信聊天记录',
    description = '查询前请先联系管理员开通 impart 权限',
    question = '如果我已获得管理员授权，请帮我检索微信 impart 聊天记录里谁提到过安装问题？',
    sort_order = 2,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 2;

update t_sample_question
set title = 'QQ 聊天记录',
    description = '查询前请先联系管理员开通 yys 权限',
    question = '如果我已获得管理员授权，请帮我检索 QQ 阴阳师群聊记录里 2022 年 1 月谁提到过游戏下载？',
    sort_order = 3,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 3;
