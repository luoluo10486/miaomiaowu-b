-- Author: OpenAI Codex
-- Date: 2026-06-07
-- Description: Expand t_user.user_type capacity for multi-role expressions and normalize common legacy values.

alter table if exists t_user
    alter column user_type type varchar(128);

update t_user
set user_type = 'superadmin'
where lower(coalesce(user_type, '')) like '%superadmin%'
   or lower(coalesce(user_type, '')) like '%admin%';

update t_user
set user_type = 'user'
where coalesce(trim(user_type), '') = '';
