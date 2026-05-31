select
    username,
    email,
    role,
    created_at,
    updated_at
from users
order by created_at desc
limit 30;

select
    r.id,
    u.username,
    r.candidate_name,
    r.status,
    left(r.raw_content_reference, 120) as raw_content_reference,
    r.created_at,
    r.updated_at
from resumes r
left join users u on u.id = r.user_id
order by r.updated_at desc
limit 30;

select
    j.id,
    j.title,
    u.username as created_by_username,
    j.created_at,
    j.updated_at
from jobs j
left join users u on u.id = j.created_by
order by j.created_at desc
limit 30;

select
    ja.id,
    u.username,
    j.title,
    ja.status,
    ja.review_note,
    ja.created_at,
    ja.updated_at
from job_applications ja
join users u on u.id = ja.user_id
join jobs j on j.id = ja.job_id
order by ja.updated_at desc
limit 30;

select
    j.title,
    r.candidate_name,
    u.username,
    jr.match_score,
    jr.created_at,
    jr.updated_at
from job_recommendations jr
join jobs j on j.id = jr.job_id
join resumes r on r.id = jr.resume_id
left join users u on u.id = r.user_id
order by jr.updated_at desc, jr.match_score desc
limit 50;