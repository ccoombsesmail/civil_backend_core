CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE sentiment AS ENUM ('POSITIVE', 'NEUTRAL', 'NEGATIVE', 'MEME');


CREATE TABLE "users" (
  id SERIAL PRIMARY KEY,
  user_id text NOT NULL,
  email varchar(50),
  username varchar(50) NOT NULL,
  tag varchar(50) UNIQUE,
  civility NUMERIC(4, 1) DEFAULT 0,
  created_at BIGINT NOT NULL,
  icon_src text,
  consortium_member boolean DEFAULT false,
  is_did_user boolean,
  UNIQUE(user_id),
  UNIQUE(username),
  UNIQUE(email)
);

CREATE INDEX user_id_users_index ON "users" (user_id);
CREATE INDEX tag_users_index ON "users" (tag);



CREATE TABLE topics (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    title text NOT NULL,
    description text NOT NULL,
    summary text NOT NULL,
    category varchar(50) NOT NULL,
    tweet_html text,
    yt_url text,
    content_url text,
    image_url text,
    vod_url text,
    evidence_links text[] DEFAULT '{}',
    likes integer DEFAULT 0,
    created_by text NOT NULL,
    user_id text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    report_status text NOT NULL,
    UNIQUE(title)
);

CREATE INDEX id_topics_index ON topics (id);
CREATE INDEX user_id_topics_index ON topics (user_id);


CREATE TABLE topic_vods (
  id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
  user_id text NOT NULL,
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
    user_id text,
    thumb_img_url text,
    sub_topic_key_words text[] DEFAULT '{}'::text[],
    topic_id uuid NOT NULL,
    UNIQUE(title, topic_id),
    CONSTRAINT fk_topics
      FOREIGN KEY(topic_id) 
	      REFERENCES topics(id)
);

CREATE INDEX id_sub_topics_index ON sub_topics (id);
CREATE INDEX user_id_sub_topics_index ON sub_topics (user_id);




CREATE TABLE comments (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text NOT NULL,
    user_id text NOT NULL,
    created_by text NOT NULL,
    subtopic_id uuid,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    source text,
    report_status text NOT NULL,
    CONSTRAINT fk_sub_topics
      FOREIGN KEY(subtopic_id) 
	      REFERENCES sub_topics(id)
);

CREATE INDEX id_comments_index ON comments (id);
CREATE INDEX user_id_comments_index ON comments (user_id);
CREATE INDEX parent_id_comments_index ON comments (parent_id);


CREATE TABLE tribunal_comments (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text NOT NULL,
    user_id text NOT NULL,
    created_by text NOT NULL,
    reported_content_id uuid,
    sentiment text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    likes  integer DEFAULT 0,
    root_id uuid,
    parent_id uuid,
    source text,
    comment_type varchar(50) NOT NULL
);

CREATE INDEX id_tribunal_comments_index ON tribunal_comments (id);
CREATE INDEX user_id_tribunal_comments_index ON tribunal_comments (user_id);
CREATE INDEX parent_id_tribunal_comments_index ON tribunal_comments (parent_id);



CREATE TABLE comment_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    value int DEFAULT 0,
    UNIQUE(comment_id, user_id)
);

CREATE INDEX user_id_comment_likes_index ON comment_likes (user_id);
CREATE INDEX comment_id_comment_likes_index ON comment_likes (comment_id);

CREATE TABLE topic_likes (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    topic_id uuid NOT NULL,
    value int DEFAULT 0,
    UNIQUE(topic_id, user_id)
);

CREATE INDEX user_id_topic_likes_index ON topic_likes (user_id);
CREATE INDEX topic_id_topic_likes_index ON topic_likes (topic_id);



CREATE TABLE comment_civility (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    comment_id uuid NOT NULL,
    value NUMERIC(4, 1) DEFAULT 0,
    UNIQUE(comment_id , user_id)
);

CREATE INDEX user_id_comment_civility_index ON comment_civility (user_id);
CREATE INDEX comment_id_comment_civility_index ON comment_civility (comment_id);


CREATE TABLE follows (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    followed_user_id text NOT NULL
);



CREATE INDEX user_id_follows_index ON follows (user_id);
CREATE INDEX follower_id_follows_index ON follows (followed_user_id);



CREATE TABLE reports (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    toxic boolean,
    spam boolean,
    personal_attack boolean,
    content_type varchar(10),
    UNIQUE(content_id, user_id)
);

CREATE INDEX user_id_reports_index ON reports (user_id);
CREATE INDEX content_id_reports_index ON reports (content_id);

CREATE TABLE report_timing (
    id SERIAL PRIMARY KEY,
    content_id uuid NOT NULL,
    report_period_start TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    report_period_end bigint NOT NULL,
    UNIQUE(content_id)
);

CREATE INDEX content_id_report_timing_index ON report_timing (content_id);

CREATE TABLE tribunal_jury(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    content_type varchar(10),
    UNIQUE(content_id, user_id)
);

CREATE INDEX user_id_tribunal_jury_index ON tribunal_jury (user_id);
CREATE INDEX content_id_tribunal_jury_index ON tribunal_jury (content_id);



CREATE TABLE tribunal_votes(
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    content_id uuid NOT NULL,
    vote_against boolean,
    vote_for boolean,
    check ( num_nonnulls(vote_against, vote_for) = 1),
    UNIQUE(content_id, user_id)
);


CREATE INDEX user_id_tribunal_votes_index ON tribunal_votes (user_id);
CREATE INDEX content_id_tribunal_votes_index ON tribunal_votes (content_id);


--  psql -U postgres -h 127.0.0.1 -d civil -f src/main/resources/init.sql
