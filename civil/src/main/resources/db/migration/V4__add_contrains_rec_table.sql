ALTER TABLE opposing_recommendations
ADD CONSTRAINT chk_only_one_is_not_null CHECK (num_nonnulls(recommended_content_id, external_recommended_content) = 1);


CREATE INDEX target_content_id_opposing_recommendations_index ON opposing_recommendations (target_content_id);
