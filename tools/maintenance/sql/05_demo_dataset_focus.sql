select
    username,
    email,
    role,
    created_at
from users
where username in (
    'admin',
    'demo_hr',
    'demo_c01',
    'demo_c02',
    'demo_c03',
    'demo_c04',
    'demo_c05',
    'demo_c06',
    'demo_c07',
    'demo_c08'
)
order by role, username;

select
    title,
    created_at,
    updated_at
from jobs
where title in (
    'Kotlin Backend Engineer',
    'Vue Frontend Engineer',
    'QA Automation Engineer',
    'Data Analyst',
    'DevOps Engineer',
    'Fullstack Engineer'
)
order by title;

select
    u.username,
    r.candidate_name,
    r.status,
    left(r.raw_content_reference, 120) as raw_content_reference,
    r.updated_at
from resumes r
join users u on u.id = r.user_id
where u.username like 'demo_c%'
order by u.username;

select
    j.title,
    count(*) as recommendation_count,
    round(avg(jr.match_score), 2) as avg_match_score,
    max(jr.updated_at) as latest_recommendation_at
from job_recommendations jr
join jobs j on j.id = jr.job_id
where j.title in (
    'Kotlin Backend Engineer',
    'Vue Frontend Engineer',
    'QA Automation Engineer',
    'Data Analyst',
    'DevOps Engineer',
    'Fullstack Engineer'
)
group by j.id, j.title
order by j.title;

select
    u.username,
    j.title,
    ja.status,
    ja.updated_at
from job_applications ja
join users u on u.id = ja.user_id
join jobs j on j.id = ja.job_id
where u.username like 'demo_c%'
order by u.username, j.title;