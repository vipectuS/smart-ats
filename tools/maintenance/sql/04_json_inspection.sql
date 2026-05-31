select
    r.id,
    u.username,
    r.status,
    r.browser_preprocessed_payload is not null as has_browser_payload,
    jsonb_extract_path_text(r.parsed_data, 'basicInfo', 'fullName') as parsed_full_name,
    jsonb_extract_path_text(r.parsed_data, 'basicInfo', 'headline') as parsed_headline,
    jsonb_extract_path_text(r.parsed_data, 'basicInfo', 'summary') as parsed_summary,
    jsonb_array_length(coalesce(r.parsed_data -> 'skills', '[]'::jsonb)) as parsed_skill_count,
    r.updated_at
from resumes r
left join users u on u.id = r.user_id
order by r.updated_at desc;

select
    r.id,
    u.username,
    jsonb_extract_path_text(r.browser_preprocessed_payload, 'sourceFileName') as source_file_name,
    jsonb_extract_path_text(r.browser_preprocessed_payload, 'engine') as engine,
    jsonb_extract_path_text(r.browser_preprocessed_payload, 'mode') as mode,
    jsonb_extract_path_text(r.browser_preprocessed_payload, 'pageCount') as page_count,
    left(jsonb_extract_path_text(r.browser_preprocessed_payload, 'extractedTextPreview'), 200) as extracted_text_preview,
    r.updated_at
from resumes r
left join users u on u.id = r.user_id
where r.browser_preprocessed_payload is not null
order by r.updated_at desc;

select
    j.title,
    r.candidate_name,
    jr.match_score,
    jr.xai_report is not null as has_xai_report,
    jsonb_extract_path_text(jr.xai_report, 'headline') as xai_headline,
    jsonb_extract_path_text(jr.xai_report, 'fitBand') as xai_fit_band,
    jr.updated_at
from job_recommendations jr
join jobs j on j.id = jr.job_id
join resumes r on r.id = jr.resume_id
order by jr.updated_at desc, jr.match_score desc
limit 50;

select
    name,
    category,
    aliases,
    enabled,
    updated_at
from skill_dictionary
order by category nulls last, name;