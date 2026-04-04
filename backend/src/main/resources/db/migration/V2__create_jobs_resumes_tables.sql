CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements JSONB,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    candidate_name VARCHAR(255),
    contact_info VARCHAR(255),
    raw_content_reference VARCHAR(255) NOT NULL,
    parsed_data JSONB,
    embedding VECTOR(1536),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matching_records (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id),
    resume_id UUID REFERENCES resumes(id),
    match_score FLOAT,
    xai_reasoning TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
