CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE "users" (
  id SERIAL PRIMARY KEY,
  clerk_id text NOT NULL,
  email text NOT NULL,
  username text NOT NULL,
  civility integer DEFAULT 0,
  created_at BIGINT NOT NULL,
  icon_src text,
  consortium_member boolean DEFAULT false,
  UNIQUE(username),
  UNIQUE(email)
);


CREATE TABLE topics (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title text NOT NULL,
    description text NOT NULL,
    summary text NOT NULL,
    category text NOT NULL,
    tweet_html text,
    yt_url text,
    content_url text,
    image_url text,
    vod_url text,
    evidence_links text[] DEFAULT '{}',
    likes integer DEFAULT 0,
    created_by text NOT NULL,
    clerk_id text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(title)
);

CREATE TABLE topic_vods (
  id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
  clerk_id text NOT NULL,
  vod_url text NOT NULL,
  topic_id uuid NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_topics
    FOREIGN KEY(topic_id) 
      REFERENCES topics(id)
);


CREATE TABLE sub_topics (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title text NOT NULL,
    created_by text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    summary text,
    description text NOT NULL,
    tweet_html text,
    yt_url text,
    evidence_links text[] DEFAULT '{}',
    likes integer DEFAULT 0,
    image_url text,
    vod_url text,
    content_url text,
    clerk_id text,
    thumb_img_url text,
    sub_topic_key_words text[] DEFAULT '{}'::text[],
    topic_id uuid NOT NULL,
    UNIQUE(title, topic_id),
    CONSTRAINT fk_topics
      FOREIGN KEY(topic_id) 
	      REFERENCES topics(id)
);

CREATE TYPE sentiment AS ENUM ('POSITIVE', 'NEUTRAL', 'NEGATIVE', 'MEME');



CREATE TABLE comments (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text NOT NULL,
    created_by text NOT NULL,
    subtopic_id uuid,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    source text,
    CONSTRAINT fk_sub_topics
      FOREIGN KEY(subtopic_id) 
	      REFERENCES sub_topics(id)
);



CREATE TABLE comment_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    UNIQUE(comment_id, user_id)
);


CREATE TABLE topic_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    topic_id uuid NOT NULL,
    UNIQUE(topic_id, user_id)
);

CREATE TABLE comment_civility (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    is_civil boolean,
    UNIQUE(comment_id , user_id)
);

CREATE INDEX user_id_topic_likes_index ON topic_likes (user_id);
CREATE INDEX user_id_comment_likes_index ON comment_likes (user_id);


CREATE TABLE follows (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    follower_id text NOT NULL
);

CREATE INDEX user_id_follows_index ON follows (user_id);
CREATE INDEX follower_id_follows_index ON follows (follower_id);


ALTER TABLE "users"
ADD COLUMN bio text;

ALTER TABLE "users"
ADD COLUMN experience text;



CREATE TABLE opposing_recommendations (
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid,
    external_recommended_content text
);



ALTER TABLE opposing_recommendations
ADD CONSTRAINT chk_only_one_is_not_null CHECK (num_nonnulls(recommended_content_id, external_recommended_content) = 1);


CREATE INDEX target_content_id_opposing_recommendations_index ON opposing_recommendations (target_content_id);


ALTER TABLE opposing_recommendations
ADD COLUMN is_sub_topic boolean;


ALTER TABLE topics
ADD COLUMN thumbImgUrl text;


ALTER TABLE topics
ADD COLUMN thumb_img_url text;

ALTER TABLE topics
DROP COLUMN thumbImgUrl;


ALTER TABLE opposing_recommendations
ADD COLUMN similarity_score decimal DEFAULT 0;


ALTER TABLE topics
ADD COLUMN topic_words text[] DEFAULT '{}'::text[];



CREATE TABLE recommendations (
    id SERIAL PRIMARY KEY,
    target_content_id uuid NOT NULL,
    recommended_content_id uuid NOT NULL,
    similarity_score decimal DEFAULT 0
);

