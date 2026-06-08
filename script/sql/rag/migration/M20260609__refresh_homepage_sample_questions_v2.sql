-- ============================================================
-- Script type: migration
-- Version: M20260609
-- Module: rag
-- Description: Refresh homepage sample questions for chat welcome screen.
-- ============================================================

update t_sample_question
set title = 'Java 知识文档',
    description = '适合查看 Java、Spring Boot 和项目实现细节',
    question = '请结合知识库说明这个项目里的 Java 权限校验链路是怎么实现的？',
    sort_order = 1,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 1;

update t_sample_question
set title = '今日天气查询',
    description = '查询当地今天的真实天气，走 MCP 天气工具',
    question = '广州今天的天气怎么样？',
    sort_order = 2,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 2;

update t_sample_question
set title = '聊天记录检索',
    description = '可询问聊天记录相关内容，查询前请先联系管理员开通权限',
    question = '如果我已经获得管理员授权，请帮我检索聊天记录里谁提到过安装问题？',
    sort_order = 3,
    enabled = 1,
    update_time = current_timestamp
where deleted = 0
  and sort_order = 3;
