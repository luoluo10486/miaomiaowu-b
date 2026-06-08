-- ============================================================
-- Script type: rollback
-- Version: R20260608
-- Module: rag
-- Description: Drop daily question quota tables.
-- ============================================================

drop table if exists t_rag_question_quota_audit;
drop table if exists t_rag_question_quota_config;
