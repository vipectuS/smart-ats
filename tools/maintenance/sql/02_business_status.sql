select role, count(*) as user_count
from users
group by role
order by role;

select
    status,
    count(*) as resume_count,
    count(*) filter (where browser_preprocessed_payload is not null) as with_browser_payload,
    count(*) filter (where parsed_data is not null) as with_parsed_data,
    count(*) filter (where parse_failure_reason is not null) as with_failure_reason,
    min(updated_at) as oldest_update,
    max(updated_at) as latest_update
from resumes
group by status
order by status;

select
    j.title,
    count(jr.*) as recommendation_count,
    max(jr.created_at) as latest_recommendation_at
from jobs j
left join job_recommendations jr on jr.job_id = j.id
group by j.id, j.title
order by recommendation_count desc, j.title;

select status, count(*) as application_count
from job_applications
group by status
order by status;

select 'favorites' as action_type, count(*) as row_count from job_favorites
union all
select 'ignores', count(*) from job_ignores
union all
select 'candidate_profiles', count(*) from candidate_profiles
order by action_type;

select
    count(*) as enabled_skill_count,
    count(*) filter (where not enabled) as disabled_skill_count
from skill_dictionary;