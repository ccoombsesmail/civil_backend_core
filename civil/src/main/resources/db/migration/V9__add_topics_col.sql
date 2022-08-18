ALTER TABLE topics
ADD COLUMN topic_words text[] DEFAULT '{}'::text[];