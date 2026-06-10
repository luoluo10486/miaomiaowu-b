-- ============================================================
-- Script type: migration
-- Version: M20260610
-- Module: rag
-- Description: Align knowledge document related schema with current entity mappings.
-- ============================================================

alter table if exists t_knowledge_document
    add column if not exists source_file_name varchar(255) default null;

alter table if exists t_knowledge_document
    add column if not exists error_message text default null;

alter table if exists t_knowledge_document
    add column if not exists metadata_json jsonb default null;

alter table if exists t_knowledge_document_schedule_exec
    add column if not exists error_message text default null;
