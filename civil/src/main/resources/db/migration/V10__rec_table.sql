CREATE TABLE recommendations (
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid NOT NULL,
    similarity_score decimal DEFAULT 0
);

