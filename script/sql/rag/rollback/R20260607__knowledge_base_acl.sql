-- ============================================================
-- Script type: rollback
-- Version: R20260607
-- Module: rag
-- Description: Roll back knowledge base ACL columns.
-- ============================================================

drop index if exists idx_t_knowledge_base_status_deleted;
drop index if exists idx_t_knowledge_base_visibility_deleted;

alter table if exists t_knowledge_base
    drop column if exists allowed_roles;

