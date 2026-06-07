-- ============================================================
-- Script type: migration
-- Version: M20260607
-- Module: rag
-- Description: Add role-based access control columns for knowledge bases.
-- ============================================================

alter table if exists t_knowledge_base
    add column if not exists allowed_roles varchar(256) default null;

create index if not exists idx_t_knowledge_base_visibility_deleted on t_knowledge_base (visibility, deleted);
create index if not exists idx_t_knowledge_base_status_deleted on t_knowledge_base (status, deleted);

