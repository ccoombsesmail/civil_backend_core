CREATE TABLE opposing_recommendations (
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid,
    external_recommended_content text
);

