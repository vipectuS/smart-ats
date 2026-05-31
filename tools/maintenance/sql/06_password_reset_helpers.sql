-- Smart ATS password helper queries
--
-- Source-derived known defaults in the current repository:
-- 1. admin -> admin
--    seeded from backend/local/backend-local.yml in local profile.
-- 2. demo_hr, demo_c01 ... demo_c08 -> DemoPass123
--    seeded or reused by tools/maintenance/seed_demo_data.py.
--
-- Historical users such as test / candemo* do not have a recoverable plaintext password here.
-- Their hashes are BCrypt and cannot be reversed. Reset them instead.

select
    username,
    email,
    role,
    created_at,
    updated_at,
    case
        when username = 'admin' then 'known-from-local-config: admin'
        when username = 'demo_hr' then 'known-from-demo-seed: DemoPass123'
        when username like 'demo_c%' then 'known-from-demo-seed: DemoPass123'
        else 'unknown-plaintext-reset-required'
    end as password_hint
from users
order by created_at desc;

select
    username,
    left(password_hash, 7) as hash_prefix,
    case when password_hash like '$2%' then true else false end as looks_like_bcrypt
from users
order by username;

-- Optional: install pgcrypto once if you want to reset passwords directly in PostgreSQL.
-- This does not touch existing data by itself.
create extension if not exists pgcrypto;

-- Example: reset one historical user to a new password.
-- Replace CHANGE_ME_PASSWORD before executing.
-- update users
-- set password_hash = crypt('CHANGE_ME_PASSWORD', gen_salt('bf', 10)),
--     updated_at = current_timestamp
-- where username = 'test';

-- Example: reset all demo accounts back to their known demo password.
-- update users
-- set password_hash = crypt('DemoPass123', gen_salt('bf', 10)),
--     updated_at = current_timestamp
-- where username = 'demo_hr'
--    or username like 'demo_c%';

-- Example: reset local admin back to admin.
-- update users
-- set password_hash = crypt('admin', gen_salt('bf', 10)),
--     updated_at = current_timestamp
-- where username = 'admin';

-- Verify the updated users after a reset.
select
    username,
    role,
    updated_at
from users
where username = 'admin'
   or username = 'demo_hr'
   or username like 'demo_c%'
   or username in ('test', 'candemo1775404693', 'candemo1775405304')
order by username;