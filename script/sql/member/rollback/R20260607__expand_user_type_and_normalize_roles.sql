-- Author: OpenAI Codex
-- Date: 2026-06-07
-- Description: Roll back user_type column expansion when existing values still fit varchar(16).
-- Note: The data normalization performed by the forward migration is not fully reversible.

do
$$
declare
    max_len integer;
begin
    select coalesce(max(length(user_type)), 0)
    into max_len
    from t_user;

    if max_len > 16 then
        raise exception 'Rollback aborted: t_user.user_type contains values longer than 16 characters (max=%).', max_len;
    end if;

    alter table if exists t_user
        alter column user_type type varchar(16);
end
$$;
