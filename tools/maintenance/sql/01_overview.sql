select current_database() as database_name, current_user as current_user;

select extname
from pg_extension
order by extname;

select table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;

select 'users' as table_name, count(*) as row_count from users
union all
select 'candidate_profiles', count(*) from candidate_profiles
union all
select 'jobs', count(*) from jobs
union all
select 'resumes', count(*) from resumes
union all
select 'job_applications', count(*) from job_applications
union all
select 'job_recommendations', count(*) from job_recommendations
union all
select 'job_favorites', count(*) from job_favorites
union all
select 'job_ignores', count(*) from job_ignores
union all
select 'matching_records', count(*) from matching_records
union all
select 'skill_dictionary', count(*) from skill_dictionary
order by table_name;

select
    table_name,
    column_name,
    data_type,
    udt_name
from information_schema.columns
where table_schema = 'public'
  and table_name in (
    'users',
    'candidate_profiles',
    'jobs',
    'resumes',
    'job_applications',
    'job_recommendations',
    'job_favorites',
    'job_ignores',
    'skill_dictionary'
  )
order by table_name, ordinal_position;