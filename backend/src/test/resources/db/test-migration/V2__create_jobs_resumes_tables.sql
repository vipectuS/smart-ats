CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements JSON,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    candidate_name VARCHAR(255),
    contact_info VARCHAR(255),
    raw_content_reference VARCHAR(255) NOT NULL,
    parsed_data JSON,
    embedding VARCHAR(4096),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matching_records (
    id UUID PRIMARY KEY,
    job_id UUID,
    resume_id UUID,
    match_score DOUBLE PRECISION,
    xai_reasoning TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matching_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_matching_resume FOREIGN KEY (resume_id) REFERENCES resumes(id)
);