-- ============================================================
-- Script type: patch
-- Version: P20260608
-- Scope: rebuild
-- Description: Align a server rebuilt from the older rag baseline to the
--              current export-backed schema without dropping data again.
-- Notes:
--   1. Safe to run multiple times.
--   2. Intended for servers that already executed the old
--      B20260423__rag_schema.sql and now need the export-backed columns.
-- ============================================================

alter table if exists t_knowledge_base
    add column if not exists dimension integer;

alter table if exists t_knowledge_base
    add column if not exists metric_type varchar(32);

alter table if exists t_knowledge_base
    add column if not exists allowed_roles varchar(256);

alter table if exists t_knowledge_document
    add column if not exists source_file_name varchar(255);

alter table if exists t_knowledge_document
    add column if not exists error_message text;

alter table if exists t_knowledge_document
    add column if not exists metadata_json jsonb;

alter table if exists t_knowledge_document_schedule_exec
    add column if not exists error_message text;

comment on column t_knowledge_base.visibility is '可见性：PRIVATE/PUBLIC/ROLES';
comment on column t_knowledge_base.dimension is '向量维度，未指定时可为空';
comment on column t_knowledge_base.metric_type is '向量距离度量类型，如 cosine/l2/ip';
comment on column t_knowledge_base.allowed_roles is '允许访问的角色列表，多个角色用英文逗号分隔';

comment on column t_knowledge_document.source_file_name is '原始文件名';
comment on column t_knowledge_document.error_message is '错误信息';
comment on column t_knowledge_document.metadata_json is '扩展元数据JSON';

comment on column t_knowledge_document_schedule_exec.error_message is '错误信息';

create index if not exists idx_t_knowledge_base_visibility_deleted
    on t_knowledge_base (visibility, deleted);

create index if not exists idx_t_knowledge_base_status_deleted
    on t_knowledge_base (status, deleted);
